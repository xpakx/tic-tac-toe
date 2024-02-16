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
    private boolean user2AI;

    private GameSymbol currentSymbol;
    private boolean firstUserStarts;

    public boolean isFirstUserTurn() {
        return (firstUserStarts && currentSymbol == GameSymbol.X) ||
                (!firstUserStarts && currentSymbol == GameSymbol.O);
    }

    public boolean isSecondUserTurn() {
        return (!firstUserStarts && currentSymbol == GameSymbol.X) ||
                (firstUserStarts && currentSymbol == GameSymbol.O);
    }

    public boolean isUserInGame(String username) {
        return username.equals(username1) ||
                (!user2AI && username.equals(username2));
    }
}
