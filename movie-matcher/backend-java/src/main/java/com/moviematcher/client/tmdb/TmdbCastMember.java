package com.moviematcher.client.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Актер из TMDB API
 */
public record TmdbCastMember(
    Long id,
    String name,
    String character,

    @JsonProperty("profile_path")
    String profilePath,

    Integer order
) {}
