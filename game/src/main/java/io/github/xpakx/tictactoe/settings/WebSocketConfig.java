package io.github.xpakx.tictactoe.settings;

import io.github.xpakx.tictactoe.security.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtUtils jwt;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config
                .setApplicationDestinationPrefixes("/app")
                .enableSimpleBroker("/topic");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/play")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    MessageHeaders headers = message.getHeaders();
                    MultiValueMap<String, String> multiValueMap = headers.get(
                            StompHeaderAccessor.NATIVE_HEADERS,MultiValueMap.class);
                    if (multiValueMap != null) {
                        for(Map.Entry<String, List<String>> head : multiValueMap.entrySet())
                        {
                            if (head.getKey().equals("Token")) {
                                final String token = head.getValue().get(0);
                                if(jwt.isInvalid(token)) {
                                    break;
                                }
                                Claims claims = jwt.getAllClaimsFromToken(token);
                                if(claims != null && claims.getSubject() != null) {
                                    accessor.setUser(claims::getSubject);
                                }
                                break;
                            }
                        }
                    }
                }
                return message;
            }
        });
    }
}
