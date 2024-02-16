package io.github.xpakx.tictactoe.clients;

import io.github.xpakx.tictactoe.clients.event.MoveEvent;
import io.github.xpakx.tictactoe.game.MoveMessage;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MovePublisher {
    private final AmqpTemplate template;
    private final String movesTopic;

    public MovePublisher(AmqpTemplate template, @Value("${amqp.exchange.moves}") String movesTopic) {
        this.template = template;
        this.movesTopic = movesTopic;
    }

    public void sendMove(MoveMessage message, String gameState, Long gameId, boolean ai) {
        MoveEvent event = new MoveEvent();
        event.setGameState(gameState);
        event.setGameId(gameId);
        event.setRow(message.getX());
        event.setColumn(message.getY());
        event.setAi(ai);
        template.convertAndSend(movesTopic, "move", event);
    }
}