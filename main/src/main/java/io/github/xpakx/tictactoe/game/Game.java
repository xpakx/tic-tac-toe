package io.github.xpakx.tictactoe.game;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.xpakx.tictactoe.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Game {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private boolean accepted;
    private String current_state;
    private GameType type;

    private boolean finished;
    private boolean won;
    private boolean lost;
    private boolean drawn;
    @Column(columnDefinition = "TIME")
    private LocalDateTime started_at;
    @Column(columnDefinition = "TIME")
    private LocalDateTime last_move_at;
    private String last_move;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opponent_id")
    @JsonIgnore
    private User opponent;
}
