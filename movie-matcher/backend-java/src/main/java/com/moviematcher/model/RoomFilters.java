package com.moviematcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RoomFilters(
    String genre,
    @JsonProperty("year_from") Integer yearFrom,
    @JsonProperty("year_to") Integer yearTo
) {}
