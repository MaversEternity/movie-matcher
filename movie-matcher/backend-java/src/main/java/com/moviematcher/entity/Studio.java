package com.moviematcher.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "studios")
public class Studio extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(unique = true, nullable = false, length = 200)
    public String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_id")
    public Country country;

    @ManyToMany(mappedBy = "studios")
    public Set<Movie> movies = new HashSet<>();

    // Custom queries
    public static Studio findByName(String name) {
        return find("name", name).firstResult();
    }
}
