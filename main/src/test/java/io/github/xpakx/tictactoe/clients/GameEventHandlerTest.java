package io.github.xpakx.tictactoe.clients;

import io.github.xpakx.tictactoe.clients.event.GameEvent;
import io.github.xpakx.tictactoe.clients.event.StateEvent;
import io.github.xpakx.tictactoe.game.Game;
import io.github.xpakx.tictactoe.game.GameRepository;
import io.github.xpakx.tictactoe.game.GameSymbol;
import io.github.xpakx.tictactoe.game.GameType;
import io.github.xpakx.tictactoe.user.User;
import io.github.xpakx.tictactoe.user.UserRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameEventHandlerTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:15.1")
    ).withDatabaseName("ttt_test");
    @Container
    static RabbitMQContainer rabbitMq = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.7.25-management-alpine")
    );

    @Autowired
    UserRepository userRepository;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    StatePublisher publisher;

    @Autowired
    RabbitTemplate rabbitTemplate;

    @Autowired
    RabbitAdmin rabbitAdmin;

    @SpyBean
    GameEventHandler gameHandler;

    Long userId;
    private boolean rabbitSetupIsDone = false;

    void config() {
        var queue = new Queue("test.queue", true);
        rabbitAdmin.declareQueue(queue);
        var exchange = ExchangeBuilder
                .topicExchange("tictactoe.state.topic")
                .durable(true)
                .build();
        rabbitAdmin.declareExchange(exchange);
        Binding binding = new Binding(
                "test.queue",
                Binding.DestinationType.QUEUE,
                "tictactoe.state.topic",
                "state",
                null
        );
        rabbitAdmin.declareBinding(binding);
        rabbitSetupIsDone = true;
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        User user = new User();
        user.setPassword("password");
        user.setUsername("test_user");
        this.userId = userRepository.save(user).getId();
        if (!rabbitSetupIsDone) {
            config();
        }
    }

    @AfterEach
    void tearDown() {
        gameRepository.deleteAll();
        userRepository.deleteAll();
        rabbitAdmin.purgeQueue("tictactoe.games.queue");
        reset(gameHandler);
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.rabbitmq.username", rabbitMq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMq::getAdminPassword);
        registry.add("spring.rabbitmq.host", rabbitMq::getHost);
        registry.add("spring.rabbitmq.port", rabbitMq::getAmqpPort);
    }

    @Test
    public void testCallingGameHandler() {
        var game = new GameEvent();
        game.setGameId(5L);
        rabbitTemplate.convertAndSend("tictactoe.games.topic", "game", game);
        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(isMessageConsumed(), Matchers.is(true));
    }

    @Test
    public void handlerShouldPublishEvent() {
        var game = new GameEvent();
        game.setGameId(5L);
        rabbitTemplate.convertAndSend("tictactoe.games.topic", "game", game);
        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(isQueueNotEmpty("test.queue"), Matchers.is(true));
    }

    @Test
    public void shouldPublishErrorForNonExistentGame() {
        var game = new GameEvent();
        game.setGameId(5L);
        rabbitTemplate.convertAndSend("tictactoe.games.topic", "game", game);
        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(isQueueNotEmpty("test.queue"), Matchers.is(true));
        var msg = getMessage("test.queue");
        assert(msg.isPresent());
        var event = msg.get();
        assertThat(event.isError(), is(true));
        assertThat(event.getErrorMessage(), containsStringIgnoringCase("no such game"));
    }

    @Test
    public void shouldPublishErrorForNonAcceptedGame() {
        var user1Id = createUser("user1");
        var user2Id = createUser("user2");
        var gameId = createGame(user1Id, user2Id, false, false);
        var game = new GameEvent();
        game.setGameId(gameId);
        rabbitTemplate.convertAndSend("tictactoe.games.topic", "game", game);
        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(isQueueNotEmpty("test.queue"), Matchers.is(true));
        var msg = getMessage("test.queue");
        assert(msg.isPresent());
        var event = msg.get();
        assertThat(event.getUsername1(), equalTo("user1"));
        assertThat(event.getUsername2(), equalTo("user2"));
        assertThat(event.getId(), equalTo(gameId));
        assertThat(event.isError(), is(true));
        assertThat(event.getErrorMessage(), containsStringIgnoringCase("not accepted"));
    }

    @Test
    public void shouldPublishErrorForFinishedGame() {
        var user1Id = createUser("user1");
        var user2Id = createUser("user2");
        var gameId = createGame(user1Id, user2Id, true, true);
        var game = new GameEvent();
        game.setGameId(gameId);
        rabbitTemplate.convertAndSend("tictactoe.games.topic", "game", game);
        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(isQueueNotEmpty("test.queue"), Matchers.is(true));
        var msg = getMessage("test.queue");
        assert(msg.isPresent());
        var event = msg.get();
        assertThat(event.getUsername1(), equalTo("user1"));
        assertThat(event.getUsername2(), equalTo("user2"));
        assertThat(event.getId(), equalTo(gameId));
        assertThat(event.isError(), is(true));
        assertThat(event.getErrorMessage(), containsStringIgnoringCase("already finished"));
    }

    @Test
    public void shouldPublishGame() {
        var user1Id = createUser("user1");
        var user2Id = createUser("user2");
        var gameId = createGame(user1Id, user2Id, true, false);
        var game = new GameEvent();
        game.setGameId(gameId);
        rabbitTemplate.convertAndSend("tictactoe.games.topic", "game", game);
        await()
                .atMost(5, TimeUnit.SECONDS)
                .until(isQueueNotEmpty("test.queue"), Matchers.is(true));
        var msg = getMessage("test.queue");
        assert(msg.isPresent());
        var event = msg.get();
        assertThat(event.getUsername1(), equalTo("user1"));
        assertThat(event.getUsername2(), equalTo("user2"));
        assertThat(event.getId(), equalTo(gameId));
        assertThat(event.isError(), is(false));
    }

    private Callable<Boolean> isMessageConsumed() {
        return () ->
            mockingDetails(gameHandler).getInvocations().stream()
                    .anyMatch(invocation -> invocation.getMethod().getName().equals("handleGame"));
    }

    private int getMessageCount(String queueName) {
        Properties queueProperties = rabbitAdmin.getQueueProperties(queueName);
        if (queueProperties != null) {
            return (int) queueProperties.get("QUEUE_MESSAGE_COUNT");
        } else {
            return 0;
        }
    }

    private Callable<Boolean> isQueueNotEmpty(String queueName) {
        return () -> getMessageCount(queueName) > 0;
    }

    private Optional<StateEvent> getMessage(String queueName) {
        var queuedMessage = rabbitTemplate.receiveAndConvert(queueName);
        System.out.println(queuedMessage);
        if (Objects.isNull(queuedMessage)) {
            return Optional.empty();
        }
        if (queuedMessage instanceof StateEvent e) {
            return Optional.of(e);
        }
        return Optional.empty();
    }

    private Long createUser(String username) {
        User user = new User();
        user.setPassword("password");
        user.setUsername(username);
        return userRepository.save(user).getId();
    }

    private Long createGame(Long userId, Long opponentId, boolean accepted, boolean finished) {
        Game game = new Game();
        game.setUser(userRepository.getReferenceById(userId));
        game.setOpponent(userRepository.getReferenceById(opponentId));
        game.setCurrentState("?????????");
        game.setType(GameType.USER);
        game.setCurrentSymbol(GameSymbol.X);
        game.setUserStarts(true);
        game.setAccepted(accepted);
        game.setFinished(finished);
        return gameRepository.save(game).getId();
    }
}