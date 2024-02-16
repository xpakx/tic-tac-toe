package io.github.xpakx.tictactoe.game.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MoveRequest {
    private int x;
    private int y;
}
