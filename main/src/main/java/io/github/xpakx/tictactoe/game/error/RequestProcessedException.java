package io.github.xpakx.tictactoe.game.error;



import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RequestProcessedException extends RuntimeException {
    public RequestProcessedException(String message) {
        super(message);
    }
}
