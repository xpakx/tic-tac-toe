package io.github.xpakx.tictactoe.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameControllerTest {
    @LocalServerPort
    private int port;
    private String baseUrl;

    WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        baseUrl = "ws://localhost".concat(":").concat(String.valueOf(port));
        stompClient = new WebSocketStompClient(
                new StandardWebSocketClient()
        );
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
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

    private static class SessionHandler extends StompSessionHandlerAdapter {
        private final CountDownLatch latch;

        public SessionHandler(final CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public void afterConnected(StompSession session,
                                   StompHeaders connectedHeaders) {
                latch.countDown();
        }
    }
}