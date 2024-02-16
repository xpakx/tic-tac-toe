package io.github.xpakx.tictactoe.clients.event;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MoveEvent {
    private String gameState;
    private Long gameId;
    private Integer column;
    private Integer row;
    private boolean ai;
}
