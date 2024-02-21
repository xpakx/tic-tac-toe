package io.github.xpakx.tictactoe.game;

import io.github.xpakx.tictactoe.game.dto.*;
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

    @SubscribeMapping("/board/{id}")
    public GameMessage subscribeBoard(@DestinationVariable Long id) {
        return service.subscribe(id);
    }

    @MessageMapping("/chat/{id}")
    @SendTo("/topic/chat/{id}")
    public ChatMessage chat(@DestinationVariable Long id, ChatRequest request, Principal principal) {
        var msg = new ChatMessage();
        msg.setPlayer(principal.getName());
        msg.setMessage(request.getMessage());
        return msg;
    }
}
