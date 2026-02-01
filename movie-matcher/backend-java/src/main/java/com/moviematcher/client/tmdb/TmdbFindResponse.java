package com.moviematcher.client.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response для поиска по внешнему ID (IMDB)
 */
public record TmdbFindResponse(
    @JsonProperty("movie_results")
    List<TmdbSearchResult> movieResults,

    @JsonProperty("tv_results")
    List<TmdbSearchResult> tvResults
) {}
