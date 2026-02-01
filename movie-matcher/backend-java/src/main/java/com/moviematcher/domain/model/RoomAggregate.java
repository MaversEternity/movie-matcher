package com.moviematcher.domain.model;

import com.moviematcher.domain.strategy.VotingCompletionStrategy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * Room - Aggregate Root по Domain-Driven Design
 *
 * Инкапсулирует всю бизнес-логику комнаты для голосования:
 * - Управление участниками
 * - Жизненный цикл комнаты (WAITING → VOTING → COMPLETED)
 * - Запуск и управление голосованием
 * - Защита инвариантов (например, нельзя присоединиться во время голосования)
 *
 * Rich Domain Model вместо Anemic Model:
 * - Логика внутри модели, а не в сервисах
 * - Инварианты защищены
 * - Поведение инкапсулировано
 */
public class RoomAggregate {

    private static final Logger log = Logger.getLogger(RoomAggregate.class);

    private final String id;
    private final String hostId;
    private final LocalDateTime createdAt;
    private final VotingCompletionStrategy completionStrategy;

    // Состояние комнаты
    private RoomState state = RoomState.WAITING;

    // Участники комнаты
    private final List<Participant> participants = new ArrayList<>();

    // Текущая сессия голосования
    private VotingSession votingSession;

    /**
     * Фабричный метод для создания комнаты
     */
    public static RoomAggregate create(
        String id,
        String hostId,
        VotingCompletionStrategy completionStrategy
    ) {
        return new RoomAggregate(id, hostId, completionStrategy);
    }

    private RoomAggregate(
        String id,
        String hostId,
        VotingCompletionStrategy completionStrategy
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                "Room ID cannot be null or empty"
            );
        }
        if (hostId == null || hostId.isBlank()) {
            throw new IllegalArgumentException(
                "Host ID cannot be null or empty"
            );
        }
        if (completionStrategy == null) {
            throw new IllegalArgumentException(
                "Completion strategy cannot be null"
            );
        }

        this.id = id;
        this.hostId = hostId;
        this.completionStrategy = completionStrategy;
        this.createdAt = LocalDateTime.now();

        // Создаем хоста как первого участника
        Participant host = new Participant(hostId, true);
        participants.add(host);

        log.infof("Created room {} with host {}", id, hostId);
    }

    /**
     * Присоединение участника к комнате
     *
     * Бизнес-правила:
     * - Нельзя присоединиться если голосование уже идет
     * - Нельзя присоединиться дважды
     */
    public void addParticipant(String participantId) {
        if (participantId == null || participantId.isBlank()) {
            throw new IllegalArgumentException(
                "Participant ID cannot be null or empty"
            );
        }

        if (state == RoomState.VOTING) {
            throw new IllegalStateException("Cannot join room during voting");
        }

        if (state == RoomState.COMPLETED) {
            throw new IllegalStateException("Cannot join completed room");
        }

        boolean alreadyInRoom = participants
            .stream()
            .anyMatch(p -> p.getId().equals(participantId));

        if (alreadyInRoom) {
            throw new IllegalStateException("Participant already in room");
        }

        Participant participant = new Participant(participantId, false);
        participants.add(participant);

        log.infof("Participant {} joined room {}", participantId, id);
    }

    /**
     * Выход участника из комнаты
     */
    public void removeParticipant(String participantId) {
        participants.removeIf(p -> p.getId().equals(participantId));
        log.infof("Participant {} left room {}", participantId, id);
    }

    /**
     * Установка фильтров участником
     */
    public void setParticipantFilters(
        String participantId,
        MovieFilters filters
    ) {
        Participant participant = findParticipant(participantId);
        if (participant == null) {
            throw new IllegalArgumentException("Participant not found");
        }

        if (state != RoomState.WAITING) {
            throw new IllegalStateException(
                "Cannot change filters after voting started"
            );
        }

        participant.setFilters(filters);
        log.infof("Participant {} set filters in room {}", participantId, id);
    }

    /**
     * Добавление фильма участником через поиск
     */
    public void addMovieToParticipant(String participantId, String movieId) {
        Participant participant = findParticipant(participantId);
        if (participant == null) {
            throw new IllegalArgumentException("Participant not found");
        }

        if (state != RoomState.WAITING) {
            throw new IllegalStateException(
                "Cannot add movies after voting started"
            );
        }

        participant.addManuallySelectedMovie(movieId);
        log.infof(
            "Participant {} added movie {} in room {}",
            participantId,
            movieId,
            id
        );
    }

    /**
     * Участник готов к голосованию
     */
    public void markParticipantReady(String participantId) {
        Participant participant = findParticipant(participantId);
        if (participant == null) {
            throw new IllegalArgumentException("Participant not found");
        }

        if (state != RoomState.WAITING) {
            throw new IllegalStateException(
                "Voting already started or completed"
            );
        }

        participant.markReady();
        log.infof("Participant {} marked ready in room {}", participantId, id);
    }

    /**
     * Запуск голосования
     *
     * Бизнес-правила:
     * - Минимум 2 участника
     * - Все участники должны быть готовы
     * - Можно запустить только из состояния WAITING
     */
    public void startVoting() {
        if (state != RoomState.WAITING) {
            throw new IllegalStateException(
                "Voting already started or completed"
            );
        }

        if (participants.size() < 2) {
            throw new IllegalStateException(
                "Need at least 2 participants to start voting"
            );
        }

        long readyCount = participants
            .stream()
            .filter(Participant::isReadyToVote)
            .count();
        if (readyCount != participants.size()) {
            throw new IllegalStateException(
                "All participants must be ready to start voting"
            );
        }

        state = RoomState.VOTING;
        votingSession = new VotingSession(participants, completionStrategy);

        log.infof(
            "Started voting in room {} with {} participants",
            id,
            participants.size()
        );
    }

    /**
     * Записать голос участника
     */
    public void recordVote(
        String participantId,
        String movieId,
        boolean isLike
    ) {
        if (state != RoomState.VOTING) {
            throw new IllegalStateException("Voting not in progress");
        }

        if (votingSession == null) {
            throw new IllegalStateException("Voting session not initialized");
        }

        votingSession.recordVote(participantId, movieId, isLike);

        // Проверяем, завершилось ли голосование
        if (votingSession.isCompleted()) {
            state = RoomState.COMPLETED;
            log.infof("Voting completed in room {}", id);
        }
    }

    /**
     * Добавить фильмы в очередь участника (из фильтров)
     */
    public void addMoviesToParticipantQueue(
        String participantId,
        List<String> movieIds
    ) {
        if (votingSession == null) {
            throw new IllegalStateException("Voting not started");
        }

        votingSession.addMoviesToParticipant(participantId, movieIds);
    }

    /**
     * Получить следующий фильм для показа
     */
    public Optional<String> getNextMovie() {
        if (votingSession == null) {
            return Optional.empty();
        }

        return votingSession.getNextMovie();
    }

    /**
     * Проверка, нужно ли уничтожить комнату
     *
     * Комната уничтожается если:
     * - Нет участников
     * - Голосование завершено и все вышли
     */
    public boolean shouldBeDestroyed() {
        return (
            participants.isEmpty() ||
            (state == RoomState.COMPLETED && allParticipantsLeft())
        );
    }

    /**
     * Проверка возраста комнаты
     */
    public boolean isOlderThan(java.time.Duration duration) {
        return LocalDateTime.now().isAfter(createdAt.plus(duration));
    }

    /**
     * Все ли участники вышли после завершения
     */
    private boolean allParticipantsLeft() {
        // В реальности нужно отслеживать активность участников
        // Пока упрощенная логика
        return false;
    }

    /**
     * Найти участника по ID
     */
    private Participant findParticipant(String participantId) {
        return participants
            .stream()
            .filter(p -> p.getId().equals(participantId))
            .findFirst()
            .orElse(null);
    }

    /**
     * Получить количество готовых участников
     */
    public int getReadyParticipantsCount() {
        return (int) participants
            .stream()
            .filter(Participant::isReadyToVote)
            .count();
    }

    /**
     * Получить совпавшие фильмы (если голосование завершено)
     */
    public List<String> getMatchedMovies() {
        if (votingSession == null || !votingSession.isCompleted()) {
            return List.of();
        }
        return votingSession.getMatchedMovies();
    }

    /**
     * Получить список ID участников
     */
    public List<String> getParticipantIds() {
        return participants
            .stream()
            .map(Participant::getId)
            .collect(Collectors.toList());
    }

    /**
     * Получить текущее состояние комнаты
     */
    public RoomState getState() {
        return state;
    }

    /**
     * Получить ID комнаты
     */
    public String getId() {
        return id;
    }

    /**
     * Получить список участников
     */
    public List<Participant> getParticipants() {
        return new ArrayList<>(participants);
    }
}
