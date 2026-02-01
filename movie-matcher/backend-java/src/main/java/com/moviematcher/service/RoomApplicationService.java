package com.moviematcher.service;

import com.moviematcher.domain.model.*;
import com.moviematcher.domain.strategy.MajorityVotingStrategy;
import com.moviematcher.domain.strategy.UnanimousVotingStrategy;
import com.moviematcher.domain.strategy.VotingCompletionStrategy;
import com.moviematcher.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;

/**
 * Application Service для управления комнатами
 *
 * Фасад между presentation layer (WebSocket/REST) и domain layer
 * Оркестрирует работу domain моделей и инфраструктуры
 */
@ApplicationScoped
public class RoomApplicationService {

    private static final Logger log = Logger.getLogger(
        RoomApplicationService.class
    );

    // In-memory хранилище комнат (heap, не БД!)
    private final Map<String, RoomAggregate> rooms = new ConcurrentHashMap<>();

    private final WebSocketBroadcastService broadcastService;
    private final MovieSelectionService movieSelectionService;

    @jakarta.inject.Inject
    public RoomApplicationService(
        WebSocketBroadcastService broadcastService,
        MovieSelectionService movieSelectionService
    ) {
        this.broadcastService = broadcastService;
        this.movieSelectionService = movieSelectionService;
    }

    /**
     * Создать комнату
     */
    public CreateRoomResponse createRoom(
        String hostId,
        VotingCompletionType completionType
    ) {
        String roomId = UUID.randomUUID().toString();

        // Выбираем стратегию завершения голосования
        VotingCompletionStrategy strategy = switch (completionType) {
            case UNANIMOUS -> new UnanimousVotingStrategy();
            case MAJORITY -> new MajorityVotingStrategy();
        };

        // Создаем room через domain model
        RoomAggregate room = RoomAggregate.create(roomId, hostId, strategy);
        rooms.put(roomId, room);

        log.infof(
            "Created room {} with host {} and completion type {}",
            roomId,
            hostId,
            completionType
        );

        // TODO: генерация QR code и share URL
        String shareUrl = "https://movie-matcher.app/room/" + roomId;

        return new CreateRoomResponse(roomId, shareUrl);
    }

    /**
     * Присоединиться к комнате
     */
    public JoinRoomResponse joinRoom(String roomId, String participantId) {
        RoomAggregate room = rooms.get(roomId);

        if (room == null) {
            return new JoinRoomResponse(false, "Room not found");
        }

        try {
            room.addParticipant(participantId);

            // Broadcast событие
            broadcastService.broadcast(
                roomId,
                new ServerMessage.ParticipantJoined(participantId)
            );

            // Создаем RoomInfo для успешного ответа
            RoomInfo roomInfo = new RoomInfo(
                room.getId(),
                null, // фильтры пока null
                room.getParticipants().size(),
                room.getState() != RoomState.COMPLETED
            );

            log.infof("Participant {} joined room {}", participantId, roomId);
            return new JoinRoomResponse(true, "Joined successfully", roomInfo);
        } catch (IllegalStateException e) {
            log.warnf("Failed to join room {}: {}", roomId, e.getMessage());
            return new JoinRoomResponse(false, e.getMessage());
        }
    }

    /**
     * Выйти из комнаты
     */
    public void leaveRoom(String roomId, String participantId) {
        RoomAggregate room = rooms.get(roomId);
        if (room == null) return;

        room.removeParticipant(participantId);

        // Broadcast событие
        broadcastService.broadcast(
            roomId,
            new ServerMessage.ParticipantLeft(participantId)
        );

        log.infof("Participant {} left room {}", participantId, roomId);

        // Проверяем, нужно ли уничтожить комнату
        if (room.shouldBeDestroyed()) {
            rooms.remove(roomId);
            log.infof("Room {} destroyed", roomId);
        }
    }

    /**
     * Установить фильтры участника
     */
    public void setParticipantFilters(
        String roomId,
        String participantId,
        String genre,
        Integer yearFrom,
        Integer yearTo,
        java.math.BigDecimal minRating,
        String type
    ) {
        RoomAggregate room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        MovieFilters filters = MovieFilters.builder()
            .genre(genre)
            .yearFrom(yearFrom)
            .yearTo(yearTo)
            .minRating(minRating)
            .type(type)
            .build();

        room.setParticipantFilters(participantId, filters);

        log.infof(
            "Participant {} set filters in room {}",
            participantId,
            roomId
        );
    }

    /**
     * Добавить фильм через поиск
     */
    public void addMovieToParticipant(
        String roomId,
        String participantId,
        String movieId
    ) {
        RoomAggregate room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        room.addMovieToParticipant(participantId, movieId);

        log.infof(
            "Participant {} added movie {} in room {}",
            participantId,
            movieId,
            roomId
        );
    }

    /**
     * Участник готов к голосованию
     */
    public void markParticipantReady(String roomId, String participantId) {
        RoomAggregate room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        room.markParticipantReady(participantId);

        int readyCount = room.getReadyParticipantsCount();
        int totalCount = room.getParticipantIds().size();

        // Broadcast обновление готовности
        broadcastService.broadcast(
            roomId,
            new ServerMessage.ParticipantReady(
                participantId,
                readyCount,
                totalCount
            )
        );

        log.infof(
            "Participant {} ready in room {} ({}/{})",
            participantId,
            roomId,
            readyCount,
            totalCount
        );
    }

    /**
     * Начать голосование
     */
    public void startVoting(String roomId) {
        RoomAggregate room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        try {
            room.startVoting();

            // Broadcast событие
            broadcastService.broadcast(
                roomId,
                new ServerMessage.VotingStarted()
            );

            log.infof("Voting started in room {}", roomId);

            // Начинаем подачу фильмов
            movieSelectionService.startMovieStream(roomId, room);
        } catch (IllegalStateException e) {
            log.errorf(
                "Failed to start voting in room {}: {}",
                roomId,
                e.getMessage()
            );
            throw e;
        }
    }

    /**
     * Записать голос
     */
    public void recordVote(
        String roomId,
        String participantId,
        String movieId,
        boolean isLike
    ) {
        RoomAggregate room = rooms.get(roomId);
        if (room == null) {
            throw new IllegalArgumentException("Room not found");
        }

        room.recordVote(participantId, movieId, isLike);

        // Broadcast событие
        broadcastService.broadcast(
            roomId,
            new ServerMessage.VoteRecorded(participantId, movieId, isLike)
        );

        // Если голосование завершено
        if (room.getState() == RoomState.COMPLETED) {
            List<String> matchedMovieIds = room.getMatchedMovies();

            broadcastService.broadcast(
                roomId,
                new ServerMessage.VotingCompleted(matchedMovieIds)
            );

            log.infof(
                "Voting completed in room {} with {} matches",
                roomId,
                matchedMovieIds.size()
            );
        }
    }

    /**
     * Получить информацию о комнате
     */
    public Optional<RoomInfo> getRoomInfo(String roomId) {
        RoomAggregate room = rooms.get(roomId);
        if (room == null) {
            return Optional.empty();
        }

        // TODO: конвертировать RoomAggregate → RoomInfo
        return Optional.empty();
    }

    /**
     * Получить комнату (для внутреннего использования)
     */
    public Optional<RoomAggregate> getRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    /**
     * Scheduled cleanup для старых комнат
     */
    @io.quarkus.scheduler.Scheduled(every = "1m")
    void cleanupOldRooms() {
        int removed = 0;

        for (var entry : rooms.entrySet()) {
            RoomAggregate room = entry.getValue();

            if (
                room.shouldBeDestroyed() ||
                room.isOlderThan(Duration.ofHours(24))
            ) {
                rooms.remove(entry.getKey());
                removed++;
                log.infof("Removed old/empty room {}", entry.getKey());
            }
        }

        if (removed > 0) {
            log.infof("Cleaned up {} rooms", removed);
        }
    }
}
