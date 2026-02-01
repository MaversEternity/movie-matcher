package com.moviematcher.client.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Продакшн компания из TMDB API
 */
public record TmdbCompany(
    Long id,

    String name,

    @JsonProperty("logo_path")
    String logoPath,

    @JsonProperty("origin_country")
    String originCountry
) {}
