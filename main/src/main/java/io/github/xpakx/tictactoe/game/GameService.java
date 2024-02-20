package io.github.xpakx.tictactoe.game;

import io.github.xpakx.tictactoe.game.dto.AcceptRequest;
import io.github.xpakx.tictactoe.game.dto.GameRequest;
import io.github.xpakx.tictactoe.game.error.GameNotFoundException;
import io.github.xpakx.tictactoe.game.error.RequestProcessedException;
import io.github.xpakx.tictactoe.game.error.UnauthorizedGameRequestChangeException;
import io.github.xpakx.tictactoe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class GameService {
    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    public Game newGame(String username, GameRequest request) {
        if (request.getType() == GameType.AI) {
            return newGameAgainstAI(username);
        } else {
            return newGameAgainstUser(username, request.getOpponent());
        }
    }

    private Game newGameAgainstUser(String username, String opponent) {
        var newGame = new Game();
        newGame.setUser(userRepository.findByUsername(username).orElseThrow());
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
        newGame.setUser(userRepository.findByUsername(username).orElseThrow());
        newGame.setCurrentState("?????????");
        newGame.setType(GameType.AI);
        newGame.setAccepted(true);
        newGame.setCurrentSymbol(GameSymbol.X);
        Random random = new Random();
        newGame.setUserStarts(random.nextBoolean());
        return gameRepository.save(newGame);
    }

    public List<Game> getRequests(String username) {
        return gameRepository.findRequests(
                userRepository.findByUsername(username)
                        .orElseThrow()
                        .getId()
        );
    }

    public List<Game> getActiveGames(String username) {
        return gameRepository.findActiveGames(
                userRepository.findByUsername(username)
                        .orElseThrow()
                        .getId()
        );
    }

    public List<Game> getOldGames(String username) {
        return gameRepository.findFinishedGames(
                userRepository.findByUsername(username)
                        .orElseThrow()
                        .getId()
        );
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
}
