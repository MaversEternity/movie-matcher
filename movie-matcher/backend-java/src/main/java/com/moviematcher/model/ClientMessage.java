package com.moviematcher.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(
    {
        @JsonSubTypes.Type(
            value = ClientMessage.SetFilters.class,
            name = "SetFilters"
        ),
        @JsonSubTypes.Type(
            value = ClientMessage.SearchMovie.class,
            name = "SearchMovie"
        ),
        @JsonSubTypes.Type(
            value = ClientMessage.AddMovieToSelection.class,
            name = "AddMovieToSelection"
        ),
        @JsonSubTypes.Type(
            value = ClientMessage.ReadyToVote.class,
            name = "ReadyToVote"
        ),
        @JsonSubTypes.Type(value = ClientMessage.Vote.class, name = "Vote"),
        @JsonSubTypes.Type(
            value = ClientMessage.LeaveRoom.class,
            name = "LeaveRoom"
        ),
    }
)
public sealed interface ClientMessage {
    /**
     * Установить фильтры для подбора фильмов
     */
    record SetFilters(
        @JsonProperty("participant_id") String participantId,
        String genre,
        @JsonProperty("year_from") Integer yearFrom,
        @JsonProperty("year_to") Integer yearTo,
        @JsonProperty("min_rating") java.math.BigDecimal minRating,
        String type
    ) implements ClientMessage {}

    /**
     * Поиск фильма по названию
     */
    record SearchMovie(
        @JsonProperty("participant_id") String participantId,
        String query
    ) implements ClientMessage {}

    /**
     * Добавить найденный фильм в свою выборку
     */
    record AddMovieToSelection(
        @JsonProperty("participant_id") String participantId,
        @JsonProperty("movie_id") String movieId
    ) implements ClientMessage {}

    /**
     * Участник готов к голосованию
     */
    record ReadyToVote(
        @JsonProperty("participant_id") String participantId
    ) implements ClientMessage {}

    /**
     * Голосование за фильм (swipe up/down)
     */
    record Vote(
        @JsonProperty("participant_id") String participantId,
        @JsonProperty("movie_id") String movieId,
        @JsonProperty("is_like") boolean isLike // true = swipe up, false = swipe down
    ) implements ClientMessage {}

    /**
     * Выйти из комнаты
     */
    record LeaveRoom(
        @JsonProperty("participant_id") String participantId
    ) implements ClientMessage {}
}
