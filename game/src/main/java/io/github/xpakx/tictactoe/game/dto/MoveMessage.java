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

    private boolean finished;
    private boolean drawn;
    private boolean won;
    private Optional<String> winner;

    public static MoveMessage of(int x, int y, String username, GameSymbol symbol) {
        return new MoveMessage(
                username,
                x,
                y,
                true,
                true,
                symbol.toString(),
                Optional.empty(),
                false,
                false,
                false,
                Optional.empty()
        );
    }

    public static MoveMessage rejected(int x, int y, String username, GameSymbol symbol, String msg) {
        var moveMessage = of(x, y, username, symbol);
        moveMessage.setMessage(Optional.of(msg));
        moveMessage.setLegal(false);
        moveMessage.setApplied(false);
        return moveMessage;
    }

    public static MoveMessage accepted(int x, int y, String username, GameSymbol symbol) {
        var moveMessage = of(x, y, username, symbol);
        moveMessage.setApplied(false);
        return moveMessage;
    }
}
