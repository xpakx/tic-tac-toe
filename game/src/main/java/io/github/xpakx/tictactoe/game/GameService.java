package io.github.xpakx.tictactoe.game;

import io.github.xpakx.tictactoe.clients.MovePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameService {
    private final MovePublisher movePublisher;

    public MoveMessage move(Long gameId, MoveRequest move, String username) {
        var game = getGameById(gameId);

        if (game.isBlocked() || !canPlayerMove(game, move, username)) {
            return new MoveMessage(
                    username,
                    move.getX(),
                    move.getY(),
                    false
            );
        }
        game.setBlocked(true);
        var msg = new MoveMessage(
                username,
                move.getX(),
                move.getY(),
                true
        );

        movePublisher.sendMove(
                msg,
                game.getCurrentState(),
                game.getId(),
                false
        );

        return msg;
    }

    // TODO
    public GameState getGameById(Long id) {
        return new GameState();
    }

    private boolean canPlayerMove(GameState game, MoveRequest move, String username) {
        return game.isUserInGame(username) &&
                ((username.equals(game.getUsername1()) && game.isFirstUserTurn()) ||
                (username.equals(game.getUsername2()) && game.isSecondUserTurn()));
    }
}
