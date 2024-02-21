package io.github.xpakx.tictactoe.clients;

import io.github.xpakx.tictactoe.clients.event.GameEvent;
import io.github.xpakx.tictactoe.game.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameEventHandler {
    private final StatePublisher publisher;
    private final GameRepository repository;

    @RabbitListener(queues = "${amqp.queue.games}")
    void handleGame(final GameEvent event) {
        try {
            var game = repository.findWithUsersById(event.getGameId());
            game.ifPresent(publisher::sendGame);
            if (game.isEmpty()) {
                publisher.sendError("No such game!");
            }
        } catch (final Exception e) {
            throw new AmqpRejectAndDontRequeueException(e);
        }
    }
}
