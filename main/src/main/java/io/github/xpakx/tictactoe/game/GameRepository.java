package io.github.xpakx.tictactoe.game;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GameRepository extends JpaRepository<Game, Long> {
    @Query("select g, u1, u2 from Game g " +
            "left join user u1 on g.user.id = u1.id " +
            "left join user u2 on g.opponent.id = u2.id " +
            "where g.opponent.id = ?1 and g.accepted = false and g.rejected = false")
    List<Game> findRequests(Long id);

    @Query("select g, u1, u2 from Game g " +
            "left join user u1 on g.user.id = u1.id " +
            "left join user u2 on g.opponent.id = u2.id " +
            "where " +
            "(g.user.id = ?1 or g.opponent.id = ?1) " +
            "and g.accepted = true and g.finished = false")
    List<Game> findActiveGames(Long id);

    @Query("select g, u1, u2 from Game g " +
            "left join user u1 on g.user.id = u1.id " +
            "left join user u2 on g.opponent.id = u2.id " +
            "where " +
            "(g.user.id = ?1 or g.opponent.id = ?1) " +
            "and g.accepted = true and g.finished = true")
    List<Game> findFinishedGames(Long id);
}
