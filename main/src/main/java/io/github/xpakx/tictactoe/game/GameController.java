package io.github.xpakx.tictactoe.game;

import io.github.xpakx.tictactoe.game.dto.GameRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class GameController {
    private final GameService service;

    @PostMapping("/game")
    public ResponseEntity<Game> newGame(@Valid @RequestBody GameRequest request, Principal principal) {
        return ResponseEntity.ok(
                service.newGame(principal.getName(), request)
        );
    }

    @GetMapping("/game/request")
    public ResponseEntity<List<Game>> getRequests(Principal principal) {
        return ResponseEntity.ok(
                service.getRequests(principal.getName())
        );
    }

    @GetMapping("/game")
    public ResponseEntity<List<Game>> getGames(Principal principal) {
        return ResponseEntity.ok(
                service.getActiveGames(principal.getName())
        );
    }
}
