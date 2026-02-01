package com.moviematcher.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviematcher.service.MovieSearchService;
import com.moviematcher.service.RoomApplicationService;
import com.moviematcher.entity.Movie;
import com.moviematcher.model.ClientMessage;
import com.moviematcher.model.ServerMessage;
import com.moviematcher.service.WebSocketBroadcastService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.util.Optional;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * WebSocket endpoint для real-time коммуникации в комнате
 *
 * Обрабатывает все события от клиентов и отправляет обновления
 */
@ServerEndpoint("/api/rooms/{roomId}/ws")
@ApplicationScoped
public class RoomWebSocket {

    private static final Logger log = Logger.getLogger(RoomWebSocket.class);

    private final RoomApplicationService roomService;
    private final MovieSearchService movieSearchService;
    private final WebSocketBroadcastService broadcastService;
    private final ObjectMapper objectMapper;

    @jakarta.inject.Inject
    public RoomWebSocket(
        RoomApplicationService roomService,
        MovieSearchService movieSearchService,
        WebSocketBroadcastService broadcastService,
        ObjectMapper objectMapper
    ) {
        this.roomService = roomService;
        this.movieSearchService = movieSearchService;
        this.broadcastService = broadcastService;
        this.objectMapper = objectMapper;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("roomId") String roomId) {
        // Проверяем существование комнаты
        if (roomService.getRoom(roomId).isEmpty()) {
            log.errorf("Room {} not found for WebSocket connection", roomId);
            try {
                session.close(
                    new CloseReason(
                        CloseReason.CloseCodes.CANNOT_ACCEPT,
                        "Room not found"
                    )
                );
            } catch (Exception e) {
                log.errorf("Error closing session", e);
            }
            return;
        }

        broadcastService.registerSession(roomId, session);
        log.infof(
            "WebSocket connection opened for room {}, session {}",
            roomId,
            session.getId()
        );
    }

    @OnMessage
    public void onMessage(
        String message,
        Session session,
        @PathParam("roomId") String roomId
    ) {
        try {
            ClientMessage clientMessage = objectMapper.readValue(
                message,
                ClientMessage.class
            );

            log.debugf(
                "Received message in room {}: {}",
                roomId,
                clientMessage.getClass().getSimpleName()
            );

            switch (clientMessage) {
                case ClientMessage.SetFilters filters -> handleSetFilters(
                    roomId,
                    filters
                );
                case ClientMessage.SearchMovie search -> handleSearchMovie(
                    roomId,
                    search,
                    session
                );
                case ClientMessage.AddMovieToSelection add -> handleAddMovie(
                    roomId,
                    add
                );
                case ClientMessage.ReadyToVote ready -> handleReadyToVote(
                    roomId,
                    ready
                );
                case ClientMessage.Vote vote -> handleVote(roomId, vote);
                case ClientMessage.LeaveRoom leave -> handleLeaveRoom(
                    roomId,
                    leave
                );
            }
        } catch (Exception e) {
            log.errorf("Error processing message in room {}", roomId, e);
            sendErrorToSession(
                session,
                "Error processing message: " + e.getMessage()
            );
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("roomId") String roomId) {
        broadcastService.unregisterSession(roomId, session);
        log.infof(
            "WebSocket connection closed for room {}, session {}",
            roomId,
            session.getId()
        );
    }

    @OnError
    public void onError(
        Session session,
        @PathParam("roomId") String roomId,
        Throwable throwable
    ) {
        log.errorf(
            "WebSocket error for room {}, session {}",
            roomId,
            session.getId(),
            throwable
        );
    }

    // ============ Message Handlers ============

    /**
     * Установка фильтров участника
     */
    private void handleSetFilters(
        String roomId,
        ClientMessage.SetFilters filters
    ) {
        try {
            roomService.setParticipantFilters(
                roomId,
                filters.participantId(),
                filters.genre(),
                filters.yearFrom(),
                filters.yearTo(),
                filters.minRating(),
                filters.type()
            );

            log.infof(
                "Participant {} set filters in room {}",
                filters.participantId(),
                roomId
            );
        } catch (Exception e) {
            log.errorf("Error setting filters: {}", e.getMessage());
        }
    }

    /**
     * Поиск фильма
     */
    private void handleSearchMovie(
        String roomId,
        ClientMessage.SearchMovie search,
        Session session
    ) {
        try {
            log.infof(
                "Searching for movie: '{}' in room {}",
                search.query(),
                roomId
            );

            Optional<Movie> movie = movieSearchService.searchMovie(
                search.query()
            );

            if (movie.isPresent()) {
                // Отправляем результат только этому участнику
                var movieData = convertToMovieData(movie.get());
                sendToSession(session, new ServerMessage.NewMovie(movieData));

                log.infof(
                    "Found movie '{}' for participant {}",
                    movie.get().title,
                    search.participantId()
                );
            } else {
                sendErrorToSession(
                    session,
                    "Movie not found: " + search.query()
                );
            }
        } catch (Exception e) {
            log.errorf("Error searching movie: {}", e.getMessage());
            sendErrorToSession(session, "Error searching movie");
        }
    }

    /**
     * Добавление фильма в выборку
     */
    private void handleAddMovie(
        String roomId,
        ClientMessage.AddMovieToSelection add
    ) {
        try {
            roomService.addMovieToParticipant(
                roomId,
                add.participantId(),
                add.movieId()
            );

            log.infof(
                "Participant {} added movie {} to selection",
                add.participantId(),
                add.movieId()
            );
        } catch (Exception e) {
            log.errorf("Error adding movie: {}", e.getMessage());
        }
    }

    /**
     * Участник готов к голосованию
     */
    private void handleReadyToVote(
        String roomId,
        ClientMessage.ReadyToVote ready
    ) {
        try {
            roomService.markParticipantReady(roomId, ready.participantId());

            log.infof(
                "Participant {} ready in room {}",
                ready.participantId(),
                roomId
            );

            // Проверяем, все ли готовы - если да, автостарт
            var room = roomService.getRoom(roomId);
            if (room.isPresent()) {
                int readyCount = room.get().getReadyParticipantsCount();
                int totalCount = room.get().getParticipantIds().size();

                if (readyCount == totalCount && totalCount >= 2) {
                    // Все готовы - стартуем голосование
                    roomService.startVoting(roomId);
                    log.infof(
                        "Auto-starting voting in room {} (all participants ready)",
                        roomId
                    );
                }
            }
        } catch (Exception e) {
            log.errorf("Error marking ready: {}", e.getMessage());
        }
    }

    /**
     * Голосование
     */
    private void handleVote(String roomId, ClientMessage.Vote vote) {
        try {
            roomService.recordVote(
                roomId,
                vote.participantId(),
                vote.movieId(),
                vote.isLike()
            );

            log.debugf(
                "Participant {} voted {} for movie {}",
                vote.participantId(),
                vote.isLike() ? "LIKE" : "DISLIKE",
                vote.movieId()
            );
        } catch (Exception e) {
            log.errorf("Error recording vote: {}", e.getMessage());
        }
    }

    /**
     * Выход из комнаты
     */
    private void handleLeaveRoom(String roomId, ClientMessage.LeaveRoom leave) {
        roomService.leaveRoom(roomId, leave.participantId());
        log.infof("Participant {} left room {}", leave.participantId(), roomId);
    }

    // ============ Helper Methods ============

    /**
     * Отправить сообщение в конкретную сессию
     */
    private void sendToSession(Session session, ServerMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.getBasicRemote().sendText(json);
        } catch (Exception e) {
            log.errorf("Error sending message to session", e);
        }
    }

    /**
     * Отправить ошибку в конкретную сессию
     */
    private void sendErrorToSession(Session session, String errorMessage) {
        sendToSession(session, new ServerMessage.Error(errorMessage));
    }

    /**
     * Конвертация Movie entity → MovieData DTO
     */
    private com.moviematcher.model.MovieData convertToMovieData(Movie movie) {
        return new com.moviematcher.model.MovieData(
            movie.title,
            movie.year != null ? movie.year.toString() : "",
            "",
            movie.runtime != null ? movie.runtime + " min" : "",
            movie.posterUrl,
            extractDirector(movie),
            extractActors(movie),
            movie.plot,
            extractCountry(movie),
            extractGenres(movie),
            movie.imdbRating != null ? movie.imdbRating.toString() : "",
            movie.imdbId
        );
    }

    private String extractDirector(Movie movie) {
        if (movie.credits == null) return "";
        return movie.credits
            .stream()
            .filter(c -> "director".equals(c.roleType))
            .map(c -> c.person.name)
            .findFirst()
            .orElse("");
    }

    private String extractActors(Movie movie) {
        if (movie.credits == null) return "";
        return movie.credits
            .stream()
            .filter(c -> "actor".equals(c.roleType))
            .limit(3)
            .map(c -> c.person.name)
            .collect(Collectors.joining(", "));
    }

    private String extractCountry(Movie movie) {
        if (movie.countries == null || movie.countries.isEmpty()) return "";
        return movie.countries
            .stream()
            .map(c -> c.name)
            .findFirst()
            .orElse("");
    }

    private String extractGenres(Movie movie) {
        if (movie.genres == null || movie.genres.isEmpty()) return "";
        return movie.genres
            .stream()
            .map(g -> g.name)
            .collect(Collectors.joining(", "));
    }
}
