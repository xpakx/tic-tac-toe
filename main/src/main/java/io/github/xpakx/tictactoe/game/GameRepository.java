package io.github.xpakx.tictactoe.game;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long> {
    @Query("select g from Game g where g.opponent.id = ?1 and g.accepted = false and g.rejected = false")
    List<Game> findRequests(Long id);

    @Query("select g from Game g where " +
            "(g.user.id = ?1 or g.opponent.id = ?1) " +
            "and g.accepted = true and g.finished = false")
    List<Game> findActiveGames(Long id);

    @Query("select g from Game g where " +
            "(g.user.id = ?1 or g.opponent.id = ?1) " +
            "and g.accepted = true and g.finished = true")
    List<Game> findFinishedGames(Long id);
}
