package io.github.xpakx.tictactoe.game.dto;

import io.github.xpakx.tictactoe.game.Game;
import io.github.xpakx.tictactoe.game.GameSymbol;
import io.github.xpakx.tictactoe.game.GameType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameSummary {
    private Long id;
    private String currentState;
    private String lastMove;
    private GameType type;

    private boolean finished;
    private boolean won;
    private boolean lost;
    private boolean drawn;

    private String username1;
    private String username2;
    private boolean userStarts;
    private GameSymbol currentSymbol;

    public static GameSummary of(Game game) {
        var summary = new GameSummary();
        summary.setId(game.getId());
        summary.setCurrentState(game.getCurrentState()); // TODO
        summary.setLastMove(game.getLastMove()); // TODO
        summary.setType(game.getType());
        summary.setFinished(game.isFinished());
        summary.setWon(game.isWon());
        summary.setLost(game.isLost());
        summary.setDrawn(game.isDrawn());
        summary.setUsername1(game.getUser().getUsername());
        summary.setUsername2(
                game.getOpponent() != null ? game.getOpponent().getUsername() : "AI"
        );
        summary.setUserStarts(game.isUserStarts());
        summary.setCurrentSymbol(game.getCurrentSymbol());
        return summary;
    }
}
