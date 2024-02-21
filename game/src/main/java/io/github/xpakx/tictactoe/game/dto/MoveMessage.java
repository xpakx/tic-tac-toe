package io.github.xpakx.tictactoe.game.dto;

import io.github.xpakx.tictactoe.game.GameSymbol;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Optional;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MoveMessage {
    private String player;
    private int x;
    private int y;
    private boolean legal;
    private boolean applied;
    private String currentSymbol;
    private Optional<String> message;

    public static MoveMessage of(int x, int y, String username, GameSymbol symbol) {
        return new MoveMessage(username, x, y, true, true, symbol.toString(), Optional.empty());
    }

    public static MoveMessage rejected(int x, int y, String username, GameSymbol symbol, String msg) {
        return new MoveMessage(username, x, y, false, false, symbol.toString(), Optional.of(msg));
    }

    public static MoveMessage accepted(int x, int y, String username, GameSymbol symbol) {
        return new MoveMessage(username, x, y, true, false, symbol.toString(), Optional.empty());
    }
}
