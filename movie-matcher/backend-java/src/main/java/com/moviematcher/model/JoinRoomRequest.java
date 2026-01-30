package com.moviematcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record JoinRoomRequest(
    @NotBlank @JsonProperty("participant_id") String participantId
) {}
