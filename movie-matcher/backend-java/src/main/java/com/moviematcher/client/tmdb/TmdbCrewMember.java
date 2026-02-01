package com.moviematcher.client.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Член съемочной группы из TMDB API
 */
public record TmdbCrewMember(
    Long id,
    String name,
    String job,
    String department,

    @JsonProperty("profile_path")
    String profilePath
) {}
