package com.moviematcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = ClientMessage.MovieLiked.class, name = "MovieLiked"),
    @JsonSubTypes.Type(value = ClientMessage.EndMatching.class, name = "EndMatching"),
    @JsonSubTypes.Type(value = ClientMessage.LeaveRoom.class, name = "LeaveRoom")
})
public sealed interface ClientMessage {

    record MovieLiked(
        @JsonProperty("participant_id") String participantId,
        @JsonProperty("imdb_id") String imdbId
    ) implements ClientMessage {}

    record EndMatching() implements ClientMessage {}

    record LeaveRoom(@JsonProperty("participant_id") String participantId) implements ClientMessage {}
}
