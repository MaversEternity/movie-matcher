package com.moviematcher.client.tmdb;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response от TMDB API для деталей фильма
 *
 * TMDB преимущество - поддержка переводов и русского языка!
 */
public record TmdbMovieResponse(
    Long id,

    @JsonProperty("imdb_id")
    String imdbId,

    String title,

    @JsonProperty("original_title")
    String originalTitle,

    String overview,

    @JsonProperty("release_date")
    String releaseDate,

    @JsonProperty("poster_path")
    String posterPath,

    @JsonProperty("backdrop_path")
    String backdropPath,

    @JsonProperty("vote_average")
    Double voteAverage,

    @JsonProperty("vote_count")
    Integer voteCount,

    Integer runtime,

    Long budget,

    Long revenue,

    List<TmdbGenre> genres,

    @JsonProperty("production_countries")
    List<TmdbCountry> productionCountries,

    @JsonProperty("spoken_languages")
    List<TmdbLanguage> spokenLanguages,

    @JsonProperty("production_companies")
    List<TmdbCompany> productionCompanies
) {}
