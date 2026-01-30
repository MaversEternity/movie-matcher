package com.moviematcher.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_ratings")
public class UserRating extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "user_id", nullable = false, length = 100)
    public String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    public Movie movie;

    @Column(nullable = false)
    public Integer rating; // 1-10

    @Column(name = "watched_at")
    public LocalDateTime watchedAt;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    // Custom queries
    public static UserRating findByUserAndMovie(String userId, Long movieId) {
        return find("userId = ?1 and movie.id = ?2", userId, movieId).firstResult();
    }

    public static long countByUser(String userId) {
        return count("userId", userId);
    }
}
