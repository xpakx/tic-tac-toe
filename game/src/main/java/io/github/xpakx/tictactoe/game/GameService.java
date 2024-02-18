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
            return MoveMessage.rejected(move.getX(), move.getY(), username, "Game not loaded, please wait!");
        }
        var game = gameOpt.get();

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
        return createGameMessage(game);
    }

    private GameMessage createGameMessage(GameState game) {
        var msg = new GameMessage();
        msg.setError(Optional.empty());
        msg.setAi(game.isUser2AI());
        msg.setUsername1(game.getUsername1());
        msg.setUsername2(game.getUsername2());
        msg.setState(game.getCurrentState());
        msg.setLastMove(game.getLastMove());
        return msg;
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
        var msg = createGameMessage(game);
        simpMessagingTemplate.convertAndSend("/topic/board/" + game.getId(), msg);
    }
}
