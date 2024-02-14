package io.github.xpakx.tictactoe.game.dto;

import io.github.xpakx.tictactoe.game.GameType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import java.util.Objects;

@Getter
@Setter
public class GameRequest {
    @NotNull(message = "Game type cannot be null!")
    private GameType type;
    private Long opponentId;

    @AssertTrue(message = "User game request must have opponent id!")
    private boolean isOpponentIdSetForNonAIType() {
        return type != GameType.USER || opponentId != null;
    }

    @AssertTrue(message = "AI game request should not have opponent id!")
    private boolean isOpponentIdUnsetForNonUserType() {
        return type == GameType.USER || opponentId == null;
    }
}
