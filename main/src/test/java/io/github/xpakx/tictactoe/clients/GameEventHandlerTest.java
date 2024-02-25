package io.github.xpakx.tictactoe.clients;

import io.github.xpakx.tictactoe.clients.event.GameEvent;
import io.github.xpakx.tictactoe.game.GameRepository;
import io.github.xpakx.tictactoe.user.User;
import io.github.xpakx.tictactoe.user.UserRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
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

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
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

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        User user = new User();
        user.setPassword("password");
        user.setUsername("test_user");
        this.userId = userRepository.save(user).getId();

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

    private Callable<Boolean> isMessageConsumed() {
        return () ->
            mockingDetails(gameHandler).getInvocations().stream()
                    .anyMatch(invocation -> invocation.getMethod().getName().equals("handleGame"));
    }
}