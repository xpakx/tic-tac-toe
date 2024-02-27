package io.github.xpakx.tictactoe.game;

import io.github.xpakx.tictactoe.game.dto.ChatMessage;
import io.github.xpakx.tictactoe.game.dto.ChatRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.nio.file.attribute.UserPrincipal;
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
class GameControllerTest {
    @LocalServerPort
    private int port;
    private String baseUrl;

    WebSocketStompClient stompClient;

    private CompletableFuture<ChatMessage> completableMessage;

    @Autowired
    SimpMessagingTemplate simpMessagingTemplate;

    @BeforeEach
    void setUp() {
        baseUrl = "ws://localhost".concat(":").concat(String.valueOf(port));
        stompClient = new WebSocketStompClient(
                new StandardWebSocketClient()
        );
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        completableMessage = new CompletableFuture<>();
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
        session.subscribe("/topic/chat/1", new FrameHandler(latch));
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
        session.subscribe("/topic/chat/1", new FrameHandler(latch));
        var msg = new ChatRequest();
        msg.setMessage("Message");
        session.send("/topic/chat/1", msg);
        await()
                .atMost(1, SECONDS)
                .untilAsserted(() -> assertEquals(0, latch.getCount()));
        ChatMessage chatMessage = completableMessage.get(1, SECONDS);
        assertThat(chatMessage, notNullValue());
        assertThat(chatMessage.getMessage(), equalTo("Message"));
        assertThat(chatMessage.getPlayer(), equalTo("guest"));
    }

    private class FrameHandler implements StompFrameHandler {
        private final CountDownLatch latch;

        public FrameHandler(CountDownLatch latch) {
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
}