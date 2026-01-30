package com.moviematcher.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OmdbSearchResponse(
    @JsonProperty("Search") List<OmdbSearchResult> search,
    @JsonProperty("Response") String response,
    @JsonProperty("Error") String error,
    @JsonProperty("totalResults") String totalResults
) {}
