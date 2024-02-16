package io.github.xpakx.tictactoe.game;

import org.springframework.stereotype.Service;

@Service
public class GameService {
    public MoveMessage move(Long gameId, MoveRequest move, String username) {
        var game = getGameById(gameId);

        if (!canPlayerMove(game, move, username)) {
            return new MoveMessage(
                    username,
                    move.getX(),
                    move.getY(),
                    false
            );
        }

        return new MoveMessage(
                username,
                move.getX(),
                move.getY(),
                true
        );
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
