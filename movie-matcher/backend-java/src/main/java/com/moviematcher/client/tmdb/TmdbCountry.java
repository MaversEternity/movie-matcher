package com.moviematcher.client.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Страна производства из TMDB API
 */
public record TmdbCountry(
    @JsonProperty("iso_3166_1")
    String iso31661,

    String name
) {}
