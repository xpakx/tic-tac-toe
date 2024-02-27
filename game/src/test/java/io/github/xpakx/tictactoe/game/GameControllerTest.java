package io.github.xpakx.tictactoe.game;

import com.redis.testcontainers.RedisContainer;
import io.github.xpakx.tictactoe.game.dto.ChatMessage;
import io.github.xpakx.tictactoe.game.dto.ChatRequest;
import io.github.xpakx.tictactoe.game.dto.GameMessage;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class GameControllerTest {
    @LocalServerPort
    private int port;
    private String baseUrl;

    WebSocketStompClient stompClient;

    private CompletableFuture<ChatMessage> completableMessage;
    private CompletableFuture<GameMessage> completableGame;

    @Value("${jwt.secret}")
    String secret;

    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;


    @Container
    static RabbitMQContainer rabbitMq = new RabbitMQContainer(
            DockerImageName.parse("rabbitmq:3.7.25-management-alpine")
    );

    @Container
    static RedisContainer redis = new RedisContainer(
            DockerImageName.parse("redis:6.2.6-alpine")
    );

    @Autowired
    RabbitAdmin rabbitAdmin;

    @Autowired
    GameRepository gameRepository;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.username", rabbitMq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMq::getAdminPassword);
        registry.add("spring.rabbitmq.host", rabbitMq::getHost);
        registry.add("spring.rabbitmq.port", rabbitMq::getAmqpPort);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getRedisPort);
    }

    private boolean rabbitSetupIsDone = false;

    void config() {
        var queue = new Queue("test.queue", true);
        rabbitAdmin.declareQueue(queue);
        var exchange = ExchangeBuilder
                .topicExchange("tictactoe.games.topic")
                .durable(true)
                .build();
        rabbitAdmin.declareExchange(exchange);
        Binding binding = new Binding(
                "test.queue",
                Binding.DestinationType.QUEUE,
                "tictactoe.games.topic",
                "game",
                null
        );
        rabbitAdmin.declareBinding(binding);
        rabbitSetupIsDone = true;
    }

    @BeforeEach
    void setUp() {
        baseUrl = "ws://localhost".concat(":").concat(String.valueOf(port));
        stompClient = new WebSocketStompClient(
                new StandardWebSocketClient()
        );
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        completableMessage = new CompletableFuture<>();
        completableGame = new CompletableFuture<>();
        if (!rabbitSetupIsDone) {
            config();
        }
    }

    @AfterEach
    void tearDown() {
        rabbitAdmin.purgeQueue("tictactoe.state.queue");
        rabbitAdmin.purgeQueue("test.queue");
    }

    @Test
    void shouldConnectToWebsocket() throws Exception {
        StompSession session = stompClient
                .connectAsync(baseUrl + "/play/websocket", new StompSessionHandlerAdapter() {})
                .get(1, SECONDS);
        await()
                .atMost(1, SECONDS)
                .untilAsserted(() -> {
                    assertThat(session.isConnected(), equalTo(true));
                });
    }

    @Test
    void shouldSubscribeChat() throws Exception {
        StompSession session = stompClient
                .connectAsync(baseUrl + "/play/websocket", new StompSessionHandlerAdapter() {})
                .get(1, SECONDS);
        await()
                .atMost(1, SECONDS)
                .until(session::isConnected);
        CountDownLatch latch = new CountDownLatch(1);
        session.subscribe("/topic/chat/1", new ChatFrameHandler(latch));
        var msg = new ChatMessage();
        msg.setMessage("Message");
        msg.setPlayer("Guest");
        simpMessagingTemplate.convertAndSend("/topic/chat/1",  msg);
        await()
                .atMost(1, SECONDS)
                .untilAsserted(() -> assertEquals(0, latch.getCount()));
        ChatMessage chatMessage = completableMessage.get(1, SECONDS);
        assertThat(chatMessage, notNullValue());
        assertThat(chatMessage.getMessage(), equalTo("Message"));
        assertThat(chatMessage.getPlayer(), equalTo("Guest"));
    }

    @Test
    void shouldSendChatMessageByGuestUser() throws Exception {
        StompSession session = stompClient
                .connectAsync(baseUrl + "/play/websocket", new StompSessionHandlerAdapter() {})
                .get(1, SECONDS);
        await()
                .atMost(1, SECONDS)
                .until(session::isConnected);
        CountDownLatch latch = new CountDownLatch(1);
        session.subscribe("/topic/chat/1", new ChatFrameHandler(latch));
        var msg = new ChatRequest();
        msg.setMessage("Message");
        session.send("/app/chat/1", msg);
        await()
                .atMost(1, SECONDS)
                .untilAsserted(() -> assertEquals(0, latch.getCount()));
        ChatMessage chatMessage = completableMessage.get(1, SECONDS);
        assertThat(chatMessage, notNullValue());
        assertThat(chatMessage.getMessage(), equalTo("Message"));
        assertThat(chatMessage.getPlayer(), equalTo("guest"));
    }

    @Test
    void shouldSendChatMessage() throws Exception {
        StompHeaders stompHeaders = new StompHeaders();
        stompHeaders.add("Token", generateToken("test_user"));
        StompSession session = stompClient
                .connectAsync(
                        baseUrl + "/play/websocket" ,
                        new WebSocketHttpHeaders(),
                        stompHeaders,
                        new StompSessionHandlerAdapter() {}
                )
                .get(1, SECONDS);
        await()
                .atMost(1, SECONDS)
                .until(session::isConnected);
        CountDownLatch latch = new CountDownLatch(1);
        session.subscribe("/topic/chat/1", new ChatFrameHandler(latch));
        var msg = new ChatRequest();
        msg.setMessage("Message");
        session.send("/app/chat/1", msg);
        await()
                .atMost(1, SECONDS)
                .untilAsserted(() -> assertEquals(0, latch.getCount()));
        ChatMessage chatMessage = completableMessage.get(1, SECONDS);
        assertThat(chatMessage, notNullValue());
        assertThat(chatMessage.getMessage(), equalTo("Message"));
        assertThat(chatMessage.getPlayer(), equalTo("test_user"));
    }

    @Test
    void shouldSendGameOnSubscription() throws Exception {
        GameState game = new GameState();
        game.setId(1L);
        game.setUsername1("test_user");
        game.setUsername2("user2");
        game.setCurrentState("????X????");
        gameRepository.save(game);
        StompHeaders stompHeaders = new StompHeaders();
        stompHeaders.add("Token", generateToken("test_user"));
        StompSession session = stompClient
                .connectAsync(
                        baseUrl + "/play/websocket" ,
                        new WebSocketHttpHeaders(),
                        stompHeaders,
                        new StompSessionHandlerAdapter() {}
                )
                .get(1, SECONDS);
        await()
                .atMost(1, SECONDS)
                .until(session::isConnected);
        CountDownLatch latch = new CountDownLatch(1);
        session.subscribe("/app/board/1", new BoardFrameHandler(latch));
        await()
                .atMost(1, SECONDS)
                .untilAsserted(() -> assertEquals(0, latch.getCount()));
        GameMessage gameMessage = completableGame.get(1, SECONDS);
        assertThat(gameMessage, notNullValue());
        assertThat(gameMessage.getUsername2(), equalTo("user2"));
    }

    private class ChatFrameHandler implements StompFrameHandler {
        private final CountDownLatch latch;

        public ChatFrameHandler(CountDownLatch latch) {
            this.latch = latch;
        }
        @Override
        public Type getPayloadType(StompHeaders headers) {
            return ChatMessage.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            completableMessage.complete((ChatMessage) payload);
            latch.countDown();
        }

    }


    public String generateToken(String username) {
        Claims claims = Jwts.claims().setSubject(username);
        claims.put(
                "roles",
                List.of()
        );
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 60 * 1000))
                .signWith(SignatureAlgorithm.HS512, secret)
                .compact();
    }


    private class BoardFrameHandler implements StompFrameHandler {
        private final CountDownLatch latch;

        public BoardFrameHandler(CountDownLatch latch) {
            this.latch = latch;
        }
        @Override
        public Type getPayloadType(StompHeaders headers) {
            return GameMessage.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            completableGame.complete((GameMessage) payload);
            latch.countDown();
        }

    }
}