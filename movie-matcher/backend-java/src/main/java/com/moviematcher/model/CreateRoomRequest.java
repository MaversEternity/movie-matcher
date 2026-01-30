package com.moviematcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRoomRequest(
    @NotBlank @JsonProperty("host_id") String hostId,
    @NotNull RoomFilters filters
) {}
