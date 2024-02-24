package io.github.xpakx.tictactoe.game.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class UnauthorizedGameRequestChangeException extends RuntimeException {
    public UnauthorizedGameRequestChangeException(String message) {
        super(message);
    }

    public UnauthorizedGameRequestChangeException() {
        super("You cannot change this request!");
    }
}
