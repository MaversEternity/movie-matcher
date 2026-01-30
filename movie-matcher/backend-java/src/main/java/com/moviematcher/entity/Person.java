package com.moviematcher.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "people")
public class Person extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(name = "imdb_id", unique = true, length = 20)
    public String imdbId;

    @Column(nullable = false, length = 200)
    public String name;

    @Column(name = "birth_date")
    public LocalDate birthDate;

    @Column(name = "photo_url", columnDefinition = "TEXT")
    public String photoUrl;

    @Column(columnDefinition = "TEXT")
    public String bio;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @OneToMany(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true)
    public Set<MovieCredit> credits = new HashSet<>();

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    // Custom queries
    public static Person findByImdbId(String imdbId) {
        return find("imdbId", imdbId).firstResult();
    }

    public static Person findByName(String name) {
        return find("name", name).firstResult();
    }
}
