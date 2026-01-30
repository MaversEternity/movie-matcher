package com.moviematcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateRoomResponse(
    @JsonProperty("room_id") String roomId,
    @JsonProperty("join_url") String joinUrl
) {}
