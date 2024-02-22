package io.github.xpakx.tictactoe.clients.event;

import io.github.xpakx.tictactoe.game.GameSymbol;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateEvent {
    private Long gameId;
    private String currentState;
    private String lastMove;
    private boolean finished;
    private boolean won;
    private boolean lost;
    private boolean drawn;
    private GameSymbol currentSymbol;
}
