package com.moviematcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ServerMessage.ParticipantJoined.class, name = "ParticipantJoined"),
    @JsonSubTypes.Type(value = ServerMessage.ParticipantLeft.class, name = "ParticipantLeft"),
    @JsonSubTypes.Type(value = ServerMessage.MatchingStarted.class, name = "MatchingStarted"),
    @JsonSubTypes.Type(value = ServerMessage.NewMovie.class, name = "NewMovie"),
    @JsonSubTypes.Type(value = ServerMessage.LikesUpdated.class, name = "LikesUpdated"),
    @JsonSubTypes.Type(value = ServerMessage.MatchingEnded.class, name = "MatchingEnded"),
    @JsonSubTypes.Type(value = ServerMessage.StreamingEnded.class, name = "StreamingEnded"),
    @JsonSubTypes.Type(value = ServerMessage.MatchFound.class, name = "MatchFound"),
    @JsonSubTypes.Type(value = ServerMessage.Error.class, name = "Error")
})
public sealed interface ServerMessage {

    record ParticipantJoined(@JsonProperty("participant_id") String participantId) implements ServerMessage {}

    record ParticipantLeft(@JsonProperty("participant_id") String participantId) implements ServerMessage {}

    record MatchingStarted() implements ServerMessage {}

    record NewMovie(MovieData movie) implements ServerMessage {}

    record LikesUpdated(
        @JsonProperty("all_likes") List<ParticipantLikes> allLikes,
        @JsonProperty("common_likes") List<MovieData> commonLikes
    ) implements ServerMessage {}

    record MatchingEnded(
        @JsonProperty("all_likes") List<ParticipantLikes> allLikes,
        @JsonProperty("common_likes") List<MovieData> commonLikes
    ) implements ServerMessage {}

    record StreamingEnded() implements ServerMessage {}

    record MatchFound(
        @JsonProperty("all_likes") List<ParticipantLikes> allLikes,
        @JsonProperty("common_likes") List<MovieData> commonLikes
    ) implements ServerMessage {}

    record Error(String message) implements ServerMessage {}
}
