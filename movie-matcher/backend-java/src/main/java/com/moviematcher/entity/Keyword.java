package com.moviematcher.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "keywords")
public class Keyword extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(unique = true, nullable = false, length = 100)
    public String name;

    @Column(unique = true, nullable = false, length = 100)
    public String slug;

    @ManyToMany(mappedBy = "keywords")
    public Set<Movie> movies = new HashSet<>();

    // Custom queries
    public static Keyword findBySlug(String slug) {
        return find("slug", slug).firstResult();
    }

    public static Keyword findByName(String name) {
        return find("name", name).firstResult();
    }
}
