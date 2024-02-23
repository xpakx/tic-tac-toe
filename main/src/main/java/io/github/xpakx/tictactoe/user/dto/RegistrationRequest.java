package io.github.xpakx.tictactoe.user.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;

import java.util.Objects;

@Getter
@Setter
public class RegistrationRequest {
    @NotBlank(message = "Username cannot be empty")
    @Length(min=5, max=15, message = "Username length must be between 5 and 15")
    private String username;
    @NotBlank(message = "Password cannot be empty")
    private String password;
    private String passwordRe;

    @AssertTrue(message = "Passwords don't match!")
    private boolean isPasswordRepeated() {
        return Objects.equals(password, passwordRe);
    }

    @AssertTrue(message = "Username cannot start with \"AI\"!")
    private boolean isUsernameDoesNotStartWithAI() {
        return Objects.isNull(username) || !username.startsWith("AI");
    }
}
