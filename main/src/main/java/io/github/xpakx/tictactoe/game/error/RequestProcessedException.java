package io.github.xpakx.tictactoe.game.error;



import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RequestProcessedException extends RuntimeException {
    public RequestProcessedException(String message) {
        super(message);
    }
}
