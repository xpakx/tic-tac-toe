package io.github.xpakx.tictactoe.clients.event;

import io.github.xpakx.tictactoe.game.GameSymbol;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StateEvent {
    private Long id;
    private String currentState;
    private String lastMove;

    private boolean finished;

    private String username1;
    private String username2;
    private boolean user2AI;

    private GameSymbol currentSymbol;
    private boolean firstUserStarts;
    private boolean error;
    private String errorMessage;
}
