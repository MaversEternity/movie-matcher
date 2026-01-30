package com.moviematcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RoomInfo(
    String id,
    RoomFilters filters,
    @JsonProperty("participants_count") int participantsCount,
    @JsonProperty("is_active") boolean isActive
) {}
