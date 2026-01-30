package com.moviematcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record MovieData(
    String title,
    String year,
    String rated,
    String runtime,
    String poster,
    String director,
    String actors,
    String plot,
    String country,
    String genre,
    @JsonProperty("imdb_rating") String imdbRating,
    @JsonProperty("imdb_id") String imdbId
) {}
