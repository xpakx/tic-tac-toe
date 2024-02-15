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
}
