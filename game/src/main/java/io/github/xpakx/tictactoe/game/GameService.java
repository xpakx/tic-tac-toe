package io.github.xpakx.tictactoe.game;

import io.github.xpakx.tictactoe.clients.MovePublisher;
import io.github.xpakx.tictactoe.game.dto.EngineEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameService {
    private final MovePublisher movePublisher;
    private final SimpMessagingTemplate simpMessagingTemplate;

    public MoveMessage move(Long gameId, MoveRequest move, String username) {
        var game = getGameById(gameId);
        if (game == null) {
            return MoveMessage.rejected(move.getX(), move.getY(), username, "Game not loaded, please wait!");
        }

        if (game.isBlocked() || !canPlayerMove(game, move, username)) {
            return MoveMessage.rejected(move.getX(), move.getY(), username, "Cannot move now!");
        }
        game.setBlocked(true);
        var msg = MoveMessage.accepted(move.getX(), move.getY(), username);

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

    public void doMakeMove(EngineEvent event) {
        var game = getGameById(event.getGameId());
        if (!event.isLegal()) {
            game.setBlocked(false);
            simpMessagingTemplate.convertAndSend(
                    "/topic/game/" + game.getId(),
                    MoveMessage.rejected(
                            event.getRow(),
                            event.getColumn(),
                            game.getCurrentPlayer(),
                            "Move is illegal!"
                    )
            );
            return;
        }
        game.setCurrentState(event.getNewState());
        game.setLastMove(event.getMove());
        if (event.isFinished()) {
            game.setDrawn(event.isDrawn());
            if (event.isWon() && game.isFirstUserTurn()) {
                game.setWon(true);
            } else if (event.isWon() && game.isSecondUserTurn()) {
                game.setLost(true);
            }
        }
        var msg = MoveMessage.of(event.getRow(), event.getColumn(), game.getCurrentPlayer());
        game.nextPlayer();
        game.setBlocked(false);
        simpMessagingTemplate.convertAndSend("/topic/game/" + game.getId(), msg);
        if (game.aiTurn()) {
           movePublisher.sendMove(null, game.getCurrentState(), game.getId(), true);
        }
    }
}
