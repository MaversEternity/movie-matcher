package com.moviematcher.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "countries")
public class Country extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(unique = true, nullable = false, length = 2)
    public String code;

    @Column(unique = true, nullable = false, length = 100)
    public String name;

    @ManyToMany(mappedBy = "countries")
    public Set<Movie> movies = new HashSet<>();

    @OneToMany(mappedBy = "country")
    public Set<Studio> studios = new HashSet<>();

    // Custom queries
    public static Country findByCode(String code) {
        return find("code", code).firstResult();
    }

    public static Country findByName(String name) {
        return find("name", name).firstResult();
    }
}
