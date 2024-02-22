package io.github.xpakx.tictactoe.game.dto;

import io.github.xpakx.tictactoe.game.GameState;
import io.github.xpakx.tictactoe.game.GameSymbol;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Optional;

@Getter
@Setter
public class GameMessage {
    private String username1;
    private String username2;
    private boolean ai;

    private String[][] state;
    private Integer lastMoveX;
    private Integer lastMoveY;
    private String currentSymbol;
    private String currentPlayer;

    private Optional<String> error;


    public static GameMessage of(GameState game) {
        var msg = new GameMessage();
        msg.setUsername1(game.getUsername1());
        msg.setUsername2(game.getUsername2());
        msg.setAi(game.isUser2AI());
        msg.setState(stringToBoard(game.getCurrentState()));
        var lastMove = lastMoveToPair(game.getLastMove());
        if (lastMove != null) {
            msg.setLastMoveX(lastMove.row);
            msg.setLastMoveY(lastMove.column);
        }
        msg.setCurrentSymbol(game.getCurrentSymbol() == GameSymbol.X ? "X" : "O");
        msg.setCurrentPlayer(game.isFirstUserTurn() ? game.getUsername1() : game.getUsername2());
        return msg;
    }


    private static String[][] stringToBoard(String str) {
        List<String> list = str.chars()
                .mapToObj((c) -> charToSymbol((char)c))
                .toList();
        var board = new String[3][3];
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

    private static String charToSymbol(char c) {
        if(c == 'x' || c == 'X') {
            return "X";
        }
        if(c == 'o' || c == 'O') {
            return "O";
        }
        return "Empty";
    }

}
