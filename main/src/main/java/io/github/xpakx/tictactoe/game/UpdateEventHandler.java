package io.github.xpakx.tictactoe.game;

import io.github.xpakx.tictactoe.clients.StatePublisher;
import io.github.xpakx.tictactoe.clients.event.GameEvent;
import io.github.xpakx.tictactoe.game.dto.UpdateEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UpdateEventHandler {
    private final GameService service;
    private final GameRepository repository;

    @RabbitListener(queues = "${amqp.queue.updates}")
    void handleGame(final UpdateEvent event) {
        try {
            var game = repository.findById(event.getGameId());
            game.ifPresent((g) -> service.updateGame(g, event));
        } catch (final Exception e) {
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }
}
