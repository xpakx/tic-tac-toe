package io.github.xpakx.tictactoe.game.dto;

import io.github.xpakx.tictactoe.game.GameType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameRequest {
    @NotNull(message = "Game type cannot be null!")
    private GameType type;
    private String opponent;

    @AssertTrue(message = "User game request must have opponent username!")
    private boolean isOpponentIdSetForNonAIType() {
        return type != GameType.USER || opponent != null;
    }

    @AssertTrue(message = "AI game request should not have opponent username!")
    private boolean isOpponentIdUnsetForNonUserType() {
        return type == GameType.USER || opponent == null;
    }
}
