package io.github.xpakx.tictactoe.game;

import io.github.xpakx.tictactoe.clients.GamePublisher;
import io.github.xpakx.tictactoe.clients.MovePublisher;
import io.github.xpakx.tictactoe.game.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameService {
    private final MovePublisher movePublisher;
    private final GamePublisher gamePublisher;
    private final SimpMessagingTemplate simpMessagingTemplate;
    private final GameRepository repository;

    public MoveMessage move(Long gameId, MoveRequest move, String username) {
        var gameOpt = getGameById(gameId);
        if (gameOpt.isEmpty()) {
            gamePublisher.getGame(gameId); // ???
            return MoveMessage.rejected(move.getX(), move.getY(), username, GameSymbol.X, "Game not loaded, please wait!");
        }
        var game = gameOpt.get();
        if (game.isFinished()) {
            return MoveMessage.rejected(move.getX(), move.getY(), username, game.getCurrentSymbol(), "Game is finished!");
        }

        if (game.isBlocked() || !canPlayerMove(game, move, username)) {
            return MoveMessage.rejected(move.getX(), move.getY(), username, game.getCurrentSymbol(), "Cannot move now!");
        }
        game.setBlocked(true);
        repository.save(game);
        var msg = MoveMessage.accepted(move.getX(), move.getY(), username, game.getCurrentSymbol());

        movePublisher.sendMove(
                msg,
                game.getCurrentState(),
                game.getCurrentSymbol(),
                game.getId(),
                false
        );

        return msg;
    }

    public Optional<GameState> getGameById(Long id) {
        return repository.findById(id);
    }

    private boolean canPlayerMove(GameState game, MoveRequest move, String username) {
        return game.isUserInGame(username) &&
                ((username.equals(game.getUsername1()) && game.isFirstUserTurn()) ||
                (username.equals(game.getUsername2()) && game.isSecondUserTurn()));
    }

    public void doMakeMove(EngineEvent event) {
        var game = getGameById(event.getGameId()).orElseThrow();
        if (!event.isLegal()) {
            game.setBlocked(false);
            simpMessagingTemplate.convertAndSend(
                    "/topic/game/" + game.getId(),
                    MoveMessage.rejected(
                            event.getRow(),
                            event.getColumn(),
                            game.getCurrentPlayer(),
                            game.getCurrentSymbol(),
                            "Move is illegal!"
                    )
            );
            return;
        }
        game.setCurrentState(event.getNewState());
        game.setLastMove(event.getMove());
        if (event.isFinished()) {
            game.setFinished(true);
            game.setDrawn(event.isDrawn());
            if (event.isWon() && game.isFirstUserTurn()) {
                game.setWon(true);
            } else if (event.isWon() && game.isSecondUserTurn()) {
                game.setLost(true);
            }
        }
        var msg = MoveMessage.of(event.getRow(), event.getColumn(), game.getCurrentPlayer(), game.getCurrentSymbol());
        if (game.isFinished()) {
            msg.setFinished(true);
            msg.setDrawn(game.isDrawn());
            msg.setWon(game.isWon());
            msg.setWinner(game.getWinner());
        }

        game.nextPlayer();
        game.setBlocked(false);
        repository.save(game);
        simpMessagingTemplate.convertAndSend("/topic/game/" + game.getId(), msg);
        if (!game.isFinished() && game.aiTurn()) {
           movePublisher.sendMove(null, game.getCurrentState(), game.getCurrentSymbol(), game.getId(), true);
        }
    }

    public GameMessage subscribe(Long gameId) {
        var gameOpt = getGameById(gameId);
        if (gameOpt.isEmpty()) {
            // add dummy game to repo?
            gamePublisher.getGame(gameId);
            var msg = new GameMessage();
            msg.setError(Optional.of("Loading game, please wait…"));
            return msg;
        }
        var game = gameOpt.get();
        return GameMessage.of(game);
    }

    public void loadGame(StateEvent event) {
        if (event.isError()) {
            var msg = new GameMessage();
            msg.setError(Optional.of(event.getErrorMessage()));
            simpMessagingTemplate.convertAndSend("/topic/board/" + event.getId(), msg);
            return;
        }

        var game = new GameState();
        game.setId(event.getId());
        game.setCurrentState(event.getCurrentState());
        game.setLastMove(event.getLastMove());
        game.setUsername1(event.getUsername1());
        game.setUsername2(event.getUsername2());
        game.setUser2AI(event.isUser2AI());
        game.setCurrentSymbol(event.getCurrentSymbol());
        game.setFirstUserStarts(event.isFirstUserStarts());
        repository.save(game);
        var msg = GameMessage.of(game);
        simpMessagingTemplate.convertAndSend("/topic/board/" + game.getId(), msg);
        if (game.aiTurn()) {
            movePublisher.sendMove(null, game.getCurrentState(), game.getCurrentSymbol(), game.getId(), true);
        }
    }
}
