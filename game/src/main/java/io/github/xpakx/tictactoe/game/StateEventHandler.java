package io.github.xpakx.tictactoe.game;

import io.github.xpakx.tictactoe.game.dto.EngineEvent;
import io.github.xpakx.tictactoe.game.dto.StateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StateEventHandler {
    private final GameService service;

    @RabbitListener(queues = "${amqp.queue.state}")
    void handleState(final StateEvent event) {
        try {
            service.loadGame(event);
        } catch (final Exception e) {
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }
}
