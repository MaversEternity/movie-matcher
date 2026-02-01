package com.moviematcher.client.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Язык из TMDB API
 */
public record TmdbLanguage(
    @JsonProperty("iso_639_1")
    String iso6391,

    String name,

    @JsonProperty("english_name")
    String englishName
) {}
