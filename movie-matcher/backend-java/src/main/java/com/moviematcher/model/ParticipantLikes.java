package com.moviematcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ParticipantLikes(
    @JsonProperty("participant_id") String participantId,
    @JsonProperty("liked_movies") List<MovieData> likedMovies
) {}
