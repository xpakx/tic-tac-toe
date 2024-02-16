package io.github.xpakx.tictactoe.game.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EngineEvent {
    private Long gameId;
    private Integer column;
    private Integer row;
    private String newState;
    private String move;

    private boolean legal;
    private boolean finished;
    private boolean won;
    private boolean drawn;
}
