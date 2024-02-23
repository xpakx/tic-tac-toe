package io.github.xpakx.tictactoe.game;

import io.github.xpakx.tictactoe.game.dto.*;
import io.github.xpakx.tictactoe.game.error.GameNotFoundException;
import io.github.xpakx.tictactoe.game.error.RequestProcessedException;
import io.github.xpakx.tictactoe.game.error.UnauthorizedGameRequestChangeException;
import io.github.xpakx.tictactoe.game.error.UserNotFoundException;
import io.github.xpakx.tictactoe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class GameService {
    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    public NewGameResponse newGame(String username, GameRequest request) {
        Game game;
        if (request.getType() == GameType.AI) {
            game = newGameAgainstAI(username);
        } else {
            game = newGameAgainstUser(username, request.getOpponent());
        }
        return new NewGameResponse(game.getId());
    }

    private Game newGameAgainstUser(String username, String opponent) {
        var newGame = new Game();
        newGame.setUser(userRepository.findByUsername(username).orElseThrow(UserNotFoundException::new));
        newGame.setCurrentState("?????????");
        newGame.setType(GameType.USER);
        newGame.setOpponent(userRepository.findByUsername(opponent).orElseThrow());
        newGame.setCurrentSymbol(GameSymbol.X);
        Random random = new Random();
        newGame.setUserStarts(random.nextBoolean());
        return gameRepository.save(newGame);
    }

    private Game newGameAgainstAI(String username) {
        var newGame = new Game();
        newGame.setUser(userRepository.findByUsername(username).orElseThrow(UserNotFoundException::new));
        newGame.setCurrentState("?????????");
        newGame.setType(GameType.AI);
        newGame.setAccepted(true);
        newGame.setCurrentSymbol(GameSymbol.X);
        Random random = new Random();
        newGame.setUserStarts(random.nextBoolean());
        return gameRepository.save(newGame);
    }

    public List<GameSummary> getRequests(String username) {
        return gameRepository.findRequests(
                userRepository.findByUsername(username)
                        .orElseThrow()
                        .getId()
                ).stream()
                .map(GameSummary::of).toList();
    }

    public List<GameSummary> getActiveGames(String username) {
        return gameRepository.findActiveGames(
                userRepository.findByUsername(username)
                        .orElseThrow()
                        .getId()
                ).stream()
                .map(GameSummary::of).toList();
    }

    public List<GameSummary> getOldGames(String username) {
        return gameRepository.findFinishedGames(
                userRepository.findByUsername(username)
                        .orElseThrow()
                        .getId()
                ).stream()
                .map(GameSummary::of).toList();
    }

    public boolean acceptRequest(String username, Long requestId, AcceptRequest decision) {
        var game = gameRepository.findById(requestId)
                .orElseThrow(GameNotFoundException::new);
        if (game.isAccepted() || game.isRejected()) {
            throw new RequestProcessedException(
                    "Request already " + (game.isAccepted() ? "accepted!" : "rejected!")
            );
        }
        if (!game.getOpponent().getUsername().equals(username)) {
            throw new UnauthorizedGameRequestChangeException();
        }
        if (decision.isAccepted()) {
            game.setAccepted(true);
        } else {
            game.setRejected(true);
        }
        gameRepository.save(game);
        return decision.isAccepted();
    }

    public void updateGame(Game game, UpdateEvent event) {
        game.setCurrentState(event.getCurrentState());
        game.setCurrentSymbol(event.getCurrentSymbol());
        game.setLastMove(event.getLastMove());
        game.setLastMoveAt(LocalDateTime.now());
        game.setFinished(event.isFinished());
        game.setWon(event.isWon());
        game.setLost(event.isLost());
        game.setDrawn(event.isDrawn());
        gameRepository.save(game);
    }
}
