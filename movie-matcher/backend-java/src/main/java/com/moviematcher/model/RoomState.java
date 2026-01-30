package com.moviematcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record RoomState(
    String id,
    RoomFilters filters,
    List<String> participants,
    @JsonProperty("is_active") boolean isActive,
    @JsonProperty("host_id") String hostId,
    @JsonProperty("all_likes") List<ParticipantLikes> allLikes,
    @JsonProperty("common_likes") List<MovieData> commonLikes
) {}
