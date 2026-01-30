package com.moviematcher.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OmdbDetailResponse(
    @JsonProperty("Title") String title,
    @JsonProperty("Year") String year,
    @JsonProperty("Rated") String rated,
    @JsonProperty("Runtime") String runtime,
    @JsonProperty("Genre") String genre,
    @JsonProperty("Director") String director,
    @JsonProperty("Actors") String actors,
    @JsonProperty("Plot") String plot,
    @JsonProperty("Country") String country,
    @JsonProperty("Poster") String poster,
    @JsonProperty("imdbRating") String imdbRating,
    @JsonProperty("imdbID") String imdbId,
    @JsonProperty("Response") String response
) {}
