package io.github.xpakx.tictactoe.game;

import io.github.xpakx.tictactoe.game.dto.GameRequest;
import io.github.xpakx.tictactoe.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GameService {
    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    public Game newGame(String username, GameRequest request) {
        if (request.getType() == GameType.AI) {
            return newGameAgainstAI(username);
        } else {
            return newGameAgainstUser(username, request.getOpponentId());
        }
    }

    private Game newGameAgainstUser(String username, Long opponentId) {
        var newGame = new Game();
        newGame.setUser(userRepository.findByUsername(username).orElseThrow());
        newGame.setCurrentState("?????????");
        newGame.setType(GameType.USER);
        newGame.setOpponent(userRepository.getReferenceById(opponentId));
        return gameRepository.save(newGame);
    }

    private Game newGameAgainstAI(String username) {
        var newGame = new Game();
        newGame.setUser(userRepository.findByUsername(username).orElseThrow());
        newGame.setCurrentState("?????????");
        newGame.setType(GameType.AI);
        newGame.setAccepted(true);
        return gameRepository.save(newGame);
    }

}
