package com.moviematcher.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "movies")
public class Movie extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "imdb_id", unique = true, nullable = false, length = 20)
    public String imdbId;

    @Column(nullable = false, length = 500)
    public String title;

    @Column(name = "original_title", length = 500)
    public String originalTitle;

    @Column(nullable = false, length = 20)
    public String type; // movie, series, episode

    public Integer year;

    @Column(name = "release_date")
    public LocalDate releaseDate;

    public Integer runtime; // minutes

    @Column(columnDefinition = "TEXT")
    public String plot;

    @Column(name = "plot_short", length = 1000)
    public String plotShort;

    @Column(name = "poster_url", columnDefinition = "TEXT")
    public String posterUrl;

    @Column(name = "backdrop_url", columnDefinition = "TEXT")
    public String backdropUrl;

    // Ratings
    @Column(name = "imdb_rating", precision = 3, scale = 1)
    public BigDecimal imdbRating;

    @Column(name = "imdb_votes")
    public Integer imdbVotes;

    @Column(name = "metacritic_score")
    public Integer metacriticScore;

    @Column(name = "rotten_tomatoes_score")
    public Integer rottenTomatoesScore;

    // Additional data
    public Long budget;

    @Column(name = "box_office")
    public Long boxOffice;

    @Column(columnDefinition = "TEXT")
    public String awards;

    // Metadata
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    // Relationships
    @ManyToMany
    @JoinTable(
        name = "movie_genres",
        joinColumns = @JoinColumn(name = "movie_id"),
        inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    public Set<Genre> genres = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "movie_countries",
        joinColumns = @JoinColumn(name = "movie_id"),
        inverseJoinColumns = @JoinColumn(name = "country_id")
    )
    public Set<Country> countries = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "movie_languages",
        joinColumns = @JoinColumn(name = "movie_id"),
        inverseJoinColumns = @JoinColumn(name = "language_id")
    )
    public Set<Language> languages = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "movie_studios",
        joinColumns = @JoinColumn(name = "movie_id"),
        inverseJoinColumns = @JoinColumn(name = "studio_id")
    )
    public Set<Studio> studios = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "movie_keywords",
        joinColumns = @JoinColumn(name = "movie_id"),
        inverseJoinColumns = @JoinColumn(name = "keyword_id")
    )
    public Set<Keyword> keywords = new HashSet<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<MovieCredit> credits = new HashSet<>();

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Custom queries
    public static Movie findByImdbId(String imdbId) {
        return find("imdbId", imdbId).firstResult();
    }
}
