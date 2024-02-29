package io.github.xpakx.tictactoe.game;

import io.github.xpakx.tictactoe.game.dto.EngineEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EngineEventHandler {
    private final GameService service;

    @RabbitListener(queues = "${amqp.queue.engine}")
    void handleEngineEvent(final EngineEvent event) {
        try {
            service.doMakeMove(event);
        } catch (final Exception e) {
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }
}
