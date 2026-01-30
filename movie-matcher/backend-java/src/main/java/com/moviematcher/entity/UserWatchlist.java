package com.moviematcher.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_watchlist")
public class UserWatchlist extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    public String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    public Movie movie;

    @Column(name = "added_at", nullable = false)
    public LocalDateTime addedAt;

    @PrePersist
    public void prePersist() {
        addedAt = LocalDateTime.now();
    }

    // Custom queries
    public static UserWatchlist findByUserAndMovie(String userId, Long movieId) {
        return find("userId = ?1 and movie.id = ?2", userId, movieId).firstResult();
    }

    public static long countByUser(String userId) {
        return count("userId", userId);
    }
}
