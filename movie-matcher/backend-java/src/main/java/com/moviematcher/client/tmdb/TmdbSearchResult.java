package com.moviematcher.client.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Результат поиска фильма в TMDB
 */
public record TmdbSearchResult(
    Long id,

    String title,

    @JsonProperty("original_title")
    String originalTitle,

    String overview,

    @JsonProperty("poster_path")
    String posterPath,

    @JsonProperty("backdrop_path")
    String backdropPath,

    @JsonProperty("release_date")
    String releaseDate,

    @JsonProperty("vote_average")
    Double voteAverage,

    @JsonProperty("vote_count")
    Integer voteCount,

    @JsonProperty("genre_ids")
    List<Integer> genreIds,

    @JsonProperty("media_type")
    String mediaType  // "movie" или "tv"
) {}
