package io.github.xpakx.tictactoe.game.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.xpakx.tictactoe.game.GameSymbol;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    private boolean finished;
    private boolean drawn;
    private boolean won;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String winner;

    public static MoveMessage of(int x, int y, String username, GameSymbol symbol) {
        return new MoveMessage(
                username,
                x,
                y,
                true,
                true,
                symbol.toString(),
                null,
                false,
                false,
                false,
                null
        );
    }

    public static MoveMessage rejected(int x, int y, String username, GameSymbol symbol, String msg) {
        var moveMessage = of(x, y, username, symbol);
        moveMessage.setMessage(msg);
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
