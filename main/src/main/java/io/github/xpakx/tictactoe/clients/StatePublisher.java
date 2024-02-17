package io.github.xpakx.tictactoe.clients;

import io.github.xpakx.tictactoe.clients.event.StateEvent;
import io.github.xpakx.tictactoe.game.Game;
import io.github.xpakx.tictactoe.game.GameType;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StatePublisher {
    private final AmqpTemplate template;
    private final String stateTopic;

    public StatePublisher(AmqpTemplate template, @Value("${amqp.exchange.state}") String stateTopic) {
        this.template = template;
        this.stateTopic = stateTopic;
    }

    public void sendGame(Game game) {
        StateEvent event = new StateEvent();
        event.setId(game.getId());
        event.setCurrentState(game.getCurrentState());
        event.setLastMove(game.getLastMove());
        event.setFinished(game.isFinished());
        if (game.isFinished()) {
            event.setError(true);
            event.setErrorMessage("Game is already finished!");
        }
        event.setUsername1(game.getUser().getUsername());
        if (game.getOpponent() != null) {
            event.setUsername2(game.getOpponent().getUsername());
        } else {
            event.setUsername2("AI");
        }
        event.setUser2AI(game.getType() == GameType.AI);
        event.setCurrentSymbol(game.getCurrentSymbol());
        event.setFirstUserStarts(game.isUserStarts());
        template.convertAndSend(stateTopic, "state", event);
    }

    public void sendError(String msg) {
        StateEvent event = new StateEvent();
        event.setError(true);
        event.setErrorMessage(msg);
        template.convertAndSend(stateTopic, "state", event);
    }
}
