package io.github.xpakx.tictactoe.user;

import io.github.xpakx.tictactoe.user.dto.AuthenticationRequest;
import io.github.xpakx.tictactoe.user.dto.AuthenticationResponse;
import io.github.xpakx.tictactoe.user.dto.RegistrationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @Valid @RequestBody RegistrationRequest registrationRequest) {
        return new ResponseEntity<>(
                service.register(registrationRequest),
                HttpStatus.CREATED
        );
    }
}
