package io.github.xpakx.tictactoe.game;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class GameController {

    @MessageMapping("/move/{id}")
    @SendTo("/topic/game/{id}")
    public MoveMessage greeting(@DestinationVariable Long id, MoveRequest move, Principal principal) {
        return new MoveMessage(
                principal.getName(),
                move.getX(),
                move.getY()
        );
    }

}
