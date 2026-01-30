package com.moviematcher.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "movie_credits")
public class MovieCredit extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    public Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id", nullable = false)
    public Person person;

    @Column(name = "role_type", nullable = false, length = 20)
    public String roleType; // actor, director, writer, producer

    @Column(name = "character_name", length = 200)
    public String characterName;

    @Column(name = "order_index")
    public Integer orderIndex;

    // Custom queries
    public static long countByMovieAndRoleType(Long movieId, String roleType) {
        return count("movie.id = ?1 and roleType = ?2", movieId, roleType);
    }
}
