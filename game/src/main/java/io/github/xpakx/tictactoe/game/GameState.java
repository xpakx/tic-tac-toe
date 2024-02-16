package io.github.xpakx.tictactoe.game;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameState {
    private Long id;
    private String currentState;
    private String lastMove;

    private boolean finished;
    private boolean won;
    private boolean lost;
    private boolean drawn;

    private String username1;
    private String username2;
    private boolean user2IsAI;

    private GameSymbol currentSymbol;
    private boolean firstUserStarts;
}

