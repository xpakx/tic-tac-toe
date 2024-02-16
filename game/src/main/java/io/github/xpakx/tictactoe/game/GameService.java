package io.github.xpakx.tictactoe.game;

import org.springframework.stereotype.Service;

@Service
public class GameService {
    public MoveMessage move(Long gameId, MoveRequest move, String username) {
        // TODO: validate move before sending
        return new MoveMessage(
                username,
                move.getX(),
                move.getY(),
                true
        );
    }
}
