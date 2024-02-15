package io.github.xpakx.tictactoe.game;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MoveRequest {
    private int x;
    private int y;
}
