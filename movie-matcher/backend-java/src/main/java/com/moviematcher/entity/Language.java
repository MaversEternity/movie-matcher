package com.moviematcher.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "languages")
public class Language extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(unique = true, nullable = false, length = 3)
    public String code;

    @Column(unique = true, nullable = false, length = 50)
    public String name;

    @ManyToMany(mappedBy = "languages")
    public Set<Movie> movies = new HashSet<>();

    // Custom queries
    public static Language findByCode(String code) {
        return find("code", code).firstResult();
    }

    public static Language findByName(String name) {
        return find("name", name).firstResult();
    }
}
