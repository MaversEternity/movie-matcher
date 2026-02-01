package com.moviematcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    {
        @JsonSubTypes.Type(
            value = ServerMessage.ParticipantJoined.class,
            name = "ParticipantJoined"
        ),
        @JsonSubTypes.Type(
            value = ServerMessage.ParticipantLeft.class,
            name = "ParticipantLeft"
        ),
        @JsonSubTypes.Type(
            value = ServerMessage.ParticipantReady.class,
            name = "ParticipantReady"
        ),
        @JsonSubTypes.Type(
            value = ServerMessage.VotingStarted.class,
            name = "VotingStarted"
        ),
        @JsonSubTypes.Type(
            value = ServerMessage.RoomLocked.class,
            name = "RoomLocked"
        ),
        @JsonSubTypes.Type(
            value = ServerMessage.NewMovie.class,
            name = "NewMovie"
        ),
        @JsonSubTypes.Type(
            value = ServerMessage.VoteRecorded.class,
            name = "VoteRecorded"
        ),
        @JsonSubTypes.Type(
            value = ServerMessage.RoundCompleted.class,
            name = "RoundCompleted"
        ),
        @JsonSubTypes.Type(
            value = ServerMessage.VotingCompleted.class,
            name = "VotingCompleted"
        ),
        @JsonSubTypes.Type(
            value = ServerMessage.NoMoreMovies.class,
            name = "NoMoreMovies"
        ),
        @JsonSubTypes.Type(
            value = ServerMessage.LikesUpdated.class,
            name = "LikesUpdated"
        ),
        @JsonSubTypes.Type(
            value = ServerMessage.MatchingEnded.class,
            name = "MatchingEnded"
        ),
        @JsonSubTypes.Type(
            value = ServerMessage.StreamingEnded.class,
            name = "StreamingEnded"
        ),
        @JsonSubTypes.Type(
            value = ServerMessage.MatchFound.class,
            name = "MatchFound"
        ),
        @JsonSubTypes.Type(
            value = ServerMessage.MatchingStarted.class,
            name = "MatchingStarted"
        ),
        @JsonSubTypes.Type(value = ServerMessage.Error.class, name = "Error"),
    }
)
public sealed interface ServerMessage {
    record ParticipantJoined(
        @JsonProperty("participant_id") String participantId
    ) implements ServerMessage {}

    record ParticipantLeft(
        @JsonProperty("participant_id") String participantId
    ) implements ServerMessage {}

    record ParticipantReady(
        @JsonProperty("participant_id") String participantId,
        @JsonProperty("ready_count") int readyCount,
        @JsonProperty("total_count") int totalCount
    ) implements ServerMessage {}

    record VotingStarted() implements ServerMessage {}

    record RoomLocked(String message) implements ServerMessage {}

    record MatchingStarted() implements ServerMessage {}

    record NewMovie(MovieData movie) implements ServerMessage {}

    record VoteRecorded(
        @JsonProperty("participant_id") String participantId,
        @JsonProperty("movie_id") String movieId,
        @JsonProperty("is_like") boolean isLike
    ) implements ServerMessage {}

    record RoundCompleted(
        @JsonProperty("round_number") int roundNumber,
        @JsonProperty("common_likes") List<String> commonLikes
    ) implements ServerMessage {}

    record VotingCompleted(
        @JsonProperty("matched_movies") List<String> matchedMovies
    ) implements ServerMessage {}

    record NoMoreMovies() implements ServerMessage {}

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
