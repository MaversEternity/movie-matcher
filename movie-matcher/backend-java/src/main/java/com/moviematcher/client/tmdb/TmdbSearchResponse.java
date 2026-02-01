package com.moviematcher.client.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response от TMDB API для поиска фильмов
 */
public record TmdbSearchResponse(
    Integer page,

    List<TmdbSearchResult> results,

    @JsonProperty("total_pages")
    Integer totalPages,

    @JsonProperty("total_results")
    Integer totalResults
) {}
