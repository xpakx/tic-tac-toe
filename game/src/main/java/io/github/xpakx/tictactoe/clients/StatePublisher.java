package io.github.xpakx.tictactoe.clients;

import io.github.xpakx.tictactoe.clients.event.GameEvent;
import io.github.xpakx.tictactoe.clients.event.UpdateEvent;
import io.github.xpakx.tictactoe.game.GameState;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StatePublisher {
    private final AmqpTemplate template;
    private final String updatesTopic;

    public StatePublisher(AmqpTemplate template, @Value("${amqp.exchange.updates}") String updatesTopic) {
        this.template = template;
        this.updatesTopic = updatesTopic;
    }

    public void publish(GameState game) {
        UpdateEvent event = new UpdateEvent();
        event.setGameId(game.getId());
        event.setCurrentState(game.getCurrentState());
        event.setCurrentSymbol(game.getCurrentSymbol());
        event.setLastMove(game.getLastMove());
        event.setFinished(game.isFinished());
        event.setWon(game.isWon());
        event.setLost(game.isLost());
        event.setDrawn(game.isDrawn());
        template.convertAndSend(updatesTopic, "update", event);
    }
}
