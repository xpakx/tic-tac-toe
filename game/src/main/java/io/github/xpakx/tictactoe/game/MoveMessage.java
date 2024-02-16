package io.github.xpakx.tictactoe.game;

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

    public static MoveMessage of(int x, int y, String username) {
        return new MoveMessage(username, x, y, true, true);
    }

    public static MoveMessage rejected(int x, int y, String username) {
        return new MoveMessage(username, x, y, false, false);
    }

    public static MoveMessage accepted(int x, int y, String username) {
        return new MoveMessage(username, x, y, true, false);
    }
}
