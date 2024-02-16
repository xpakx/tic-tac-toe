package io.github.xpakx.tictactoe.clients;

import io.github.xpakx.tictactoe.clients.event.GameEvent;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GamePublisher {
    private final AmqpTemplate template;
    private final String gamesTopic;

    public GamePublisher(AmqpTemplate template, @Value("${amqp.exchange.games}") String gamesTopic) {
        this.template = template;
        this.gamesTopic = gamesTopic;
    }

    public void getGame(Long gameId) {
        GameEvent event = new GameEvent();
        event.setGameId(gameId);
        template.convertAndSend(gamesTopic, "game", event);
    }
}
