package io.github.xpakx.tictactoe.game;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class GameController {
    private final GameService service;

    @MessageMapping("/move/{id}")
    @SendTo("/topic/game/{id}")
    public MoveMessage move(@DestinationVariable Long id, MoveRequest move, Principal principal) {
        return service.move(id, move, principal.getName());
    }

}
