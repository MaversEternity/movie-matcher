package com.moviematcher.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "genres")
public class Genre extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(unique = true, nullable = false, length = 50)
    public String name;

    @Column(unique = true, nullable = false, length = 50)
    public String slug;

    @ManyToMany(mappedBy = "genres")
    public Set<Movie> movies = new HashSet<>();

    // Custom queries
    public static Genre findBySlug(String slug) {
        return find("slug", slug).firstResult();
    }

    public static Genre findByName(String name) {
        return find("name", name).firstResult();
    }
}
