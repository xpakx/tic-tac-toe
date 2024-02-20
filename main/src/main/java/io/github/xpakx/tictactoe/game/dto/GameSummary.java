package io.github.xpakx.tictactoe.game.dto;

import io.github.xpakx.tictactoe.game.Game;
import io.github.xpakx.tictactoe.game.GameSymbol;
import io.github.xpakx.tictactoe.game.GameType;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class GameSummary {
    private Long id;
    private GameSymbol[][] currentState;
    private Integer lastMoveRow;
    private Integer lastMoveColumn;
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
        summary.setCurrentState(stringToBoard(game.getCurrentState()));
        var lastMove = lastMoveToPair(game.getLastMove());
        if (lastMove != null) {
            summary.setLastMoveRow(lastMove.row);
            summary.setLastMoveColumn(lastMove.column);
        }
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

    private static GameSymbol[][] stringToBoard(String str) {
        List<GameSymbol> list = str.chars()
                .mapToObj((c) -> charToSymbol((char)c))
                .toList();
        GameSymbol[][] board = new GameSymbol[3][3];
        for (int row=0; row<3; row++) {
            for (int column=0; column<3; column++) {
                board[row][column] = list.get(3*row+column);
            }
        }
        return board;
    }

    private record Pair(int row, int column) { }

    private static Pair lastMoveToPair(String str) {
        if (str == null) {
            return null;
        }
        int currentRow = 0;
        int currentColumn = 0;
        for (char a : str.toCharArray()) {
            if (a != '?') {
                return new Pair(currentRow, currentColumn);
            }
            currentColumn++;
            if (currentColumn >= 3) {
                currentColumn = 0;
                currentRow++;
            }
        }
        return null;
    }

    private static GameSymbol charToSymbol(char c) {
        if(c == 'x') {
            return GameSymbol.X;
        }
        if(c == 'o') {
            return GameSymbol.O;
        }
        return GameSymbol.Empty;

    }
}
