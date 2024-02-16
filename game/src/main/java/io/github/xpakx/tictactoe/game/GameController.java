package io.github.xpakx.tictactoe.game;

import io.github.xpakx.tictactoe.game.dto.GameMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
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

    @SubscribeMapping("/topic/game/{id}")
    public GameMessage subscribe(@DestinationVariable Long id) {
        return service.subscribe(id);
    }
}
