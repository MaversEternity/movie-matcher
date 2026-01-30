package com.moviematcher.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OmdbSearchResult(
    @JsonProperty("imdbID") String imdbId,
    @JsonProperty("Type") String type
) {}
