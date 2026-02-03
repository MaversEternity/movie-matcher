package com.moviematcher.domain.model;

import static org.assertj.core.api.Assertions.*;

import com.moviematcher.domain.strategy.UnanimousVotingStrategy;
import com.moviematcher.domain.strategy.VotingCompletionStrategy;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke тесты для RoomAggregate - Aggregate Root
 *
 * Проверяют основную функциональность rich domain model:
 * - Создание комнаты
 * - Добавление/удаление участников
 * - Запуск голосования
 * - Защита инвариантов
 */
@DisplayName("RoomAggregate Smoke Tests")
class RoomAggregateTest {

    private VotingCompletionStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new UnanimousVotingStrategy();
    }

    @Test
    @DisplayName("Должен создать комнату с хостом")
    void shouldCreateRoomWithHost() {
        // Given
        String roomId = "room1";
        String hostId = "host123";

        // When
        RoomAggregate room = RoomAggregate.create(roomId, hostId, strategy);

        // Then
        assertThat(room.getId()).isEqualTo(roomId);
        assertThat(room.getState()).isEqualTo(RoomState.WAITING);
        assertThat(room.getParticipantIds()).containsExactly(hostId);
    }

    @Test
    @DisplayName("Должен добавить участника в комнату")
    void shouldAddParticipantToRoom() {
        // Given
        RoomAggregate room = RoomAggregate.create(
            "room1",
            "host123",
            strategy
        );

        // When
        room.addParticipant("participant1");

        // Then
        assertThat(room.getParticipantIds()).containsExactlyInAnyOrder(
            "host123",
            "participant1"
        );
    }

    @Test
    @DisplayName("Не должен позволить присоединиться дважды")
    void shouldNotAllowDuplicateParticipant() {
        // Given
        RoomAggregate room = RoomAggregate.create(
            "room1",
            "host123",
            strategy
        );
        room.addParticipant("participant1");

        // When / Then
        assertThatThrownBy(() -> room.addParticipant("participant1"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already in room");
    }

    @Test
    @DisplayName("Не должен позволить присоединиться во время голосования")
    void shouldNotAllowJoiningDuringVoting() {
        // Given
        RoomAggregate room = RoomAggregate.create(
            "room1",
            "host123",
            strategy
        );
        room.addParticipant("participant1");

        // Подготовка к голосованию
        room.setParticipantFilters(
            "host123",
            new MovieFilters(null, null, null, null, null)
        );
        room.setParticipantFilters(
            "participant1",
            new MovieFilters(null, null, null, null, null)
        );
        room.markParticipantReady("host123");
        room.markParticipantReady("participant1");

        // Запускаем голосование
        room.startVoting();

        // When / Then
        assertThatThrownBy(() -> room.addParticipant("participant2"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot join room during voting");
    }

    @Test
    @DisplayName("Должен удалить участника из комнаты")
    void shouldRemoveParticipant() {
        // Given
        RoomAggregate room = RoomAggregate.create(
            "room1",
            "host123",
            strategy
        );
        room.addParticipant("participant1");

        // When
        room.removeParticipant("participant1");

        // Then
        assertThat(room.getParticipantIds()).containsExactly("host123");
    }

    @Test
    @DisplayName("Должен установить фильтры участнику")
    void shouldSetParticipantFilters() {
        // Given
        RoomAggregate room = RoomAggregate.create(
            "room1",
            "host123",
            strategy
        );
        MovieFilters filters = new MovieFilters(
            "Action",
            2020,
            2023,
            java.math.BigDecimal.valueOf(7.0),
            "movie"
        );

        // When
        room.setParticipantFilters("host123", filters);

        // Then
        Participant host = room
            .getParticipants()
            .stream()
            .filter(p -> p.getId().equals("host123"))
            .findFirst()
            .orElseThrow();
        assertThat(host.getFilters()).isEqualTo(filters);
    }

    @Test
    @DisplayName("Должен добавить фильм участнику")
    void shouldAddMovieToParticipant() {
        // Given
        RoomAggregate room = RoomAggregate.create(
            "room1",
            "host123",
            strategy
        );

        // When
        room.addMovieToParticipant("host123", "movie1");

        // Then
        Participant host = room
            .getParticipants()
            .stream()
            .filter(p -> p.getId().equals("host123"))
            .findFirst()
            .orElseThrow();
        assertThat(host.hasManualMovies()).isTrue();
    }

    @Test
    @DisplayName("Должен пометить участника готовым к голосованию")
    void shouldMarkParticipantReady() {
        // Given
        RoomAggregate room = RoomAggregate.create(
            "room1",
            "host123",
            strategy
        );
        room.setParticipantFilters(
            "host123",
            new MovieFilters(null, null, null, null, null)
        );

        // When
        room.markParticipantReady("host123");

        // Then
        assertThat(room.getReadyParticipantsCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Должен запустить голосование с минимум 2 участниками")
    void shouldStartVotingWithMinimumParticipants() {
        // Given
        RoomAggregate room = RoomAggregate.create(
            "room1",
            "host123",
            strategy
        );
        room.addParticipant("participant1");

        room.setParticipantFilters(
            "host123",
            new MovieFilters(null, null, null, null, null)
        );
        room.setParticipantFilters(
            "participant1",
            new MovieFilters(null, null, null, null, null)
        );

        room.markParticipantReady("host123");
        room.markParticipantReady("participant1");

        // When
        room.startVoting();

        // Then
        assertThat(room.getState()).isEqualTo(RoomState.VOTING);
    }

    @Test
    @DisplayName("Не должен запустить голосование с 1 участником")
    void shouldNotStartVotingWithOneParticipant() {
        // Given
        RoomAggregate room = RoomAggregate.create(
            "room1",
            "host123",
            strategy
        );
        room.setParticipantFilters(
            "host123",
            new MovieFilters(null, null, null, null, null)
        );
        room.markParticipantReady("host123");

        // When / Then
        assertThatThrownBy(() -> room.startVoting())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("at least 2 participants");
    }

    @Test
    @DisplayName("Не должен запустить голосование если не все готовы")
    void shouldNotStartVotingIfNotAllReady() {
        // Given
        RoomAggregate room = RoomAggregate.create(
            "room1",
            "host123",
            strategy
        );
        room.addParticipant("participant1");

        room.setParticipantFilters(
            "host123",
            new MovieFilters(null, null, null, null, null)
        );
        room.setParticipantFilters(
            "participant1",
            new MovieFilters(null, null, null, null, null)
        );

        room.markParticipantReady("host123");
        // participant1 не готов!

        // When / Then
        assertThatThrownBy(() -> room.startVoting())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("All participants must be ready");
    }

    @Test
    @DisplayName("Должен записать голос участника")
    void shouldRecordVote() {
        // Given
        RoomAggregate room = RoomAggregate.create(
            "room1",
            "host123",
            strategy
        );
        room.addParticipant("participant1");

        room.setParticipantFilters(
            "host123",
            new MovieFilters(null, null, null, null, null)
        );
        room.setParticipantFilters(
            "participant1",
            new MovieFilters(null, null, null, null, null)
        );

        room.markParticipantReady("host123");
        room.markParticipantReady("participant1");
        room.startVoting();

        // When
        room.recordVote("host123", "movie1", true);

        // Then - голос записан, но голосование не завершено
        assertThat(room.getState()).isEqualTo(RoomState.VOTING);
    }

    @Test
    @DisplayName("Не должен позволить голосовать до начала голосования")
    void shouldNotAllowVotingBeforeStart() {
        // Given
        RoomAggregate room = RoomAggregate.create(
            "room1",
            "host123",
            strategy
        );

        // When / Then
        assertThatThrownBy(() -> room.recordVote("host123", "movie1", true))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Voting not in progress");
    }

    @Test
    @DisplayName("Должен определить старую комнату")
    void shouldIdentifyOldRoom() throws InterruptedException {
        // Given
        RoomAggregate room = RoomAggregate.create(
            "room1",
            "host123",
            strategy
        );

        // When
        Thread.sleep(100); // Ждем немного

        // Then
        assertThat(room.isOlderThan(Duration.ofMillis(50))).isTrue();
        assertThat(room.isOlderThan(Duration.ofHours(1))).isFalse();
    }

    @Test
    @DisplayName("Должен определить что комнату нужно уничтожить когда нет участников")
    void shouldBeDestroyedWhenNoParticipants() {
        // Given
        RoomAggregate room = RoomAggregate.create(
            "room1",
            "host123",
            strategy
        );

        // When
        room.removeParticipant("host123");

        // Then
        assertThat(room.shouldBeDestroyed()).isTrue();
    }

    @Test
    @DisplayName("Не должен позволить создать комнату с null ID")
    void shouldNotAllowNullRoomId() {
        assertThatThrownBy(() ->
                RoomAggregate.create(null, "host123", strategy)
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Room ID cannot be null");
    }

    @Test
    @DisplayName("Не должен позволить создать комнату с пустым ID")
    void shouldNotAllowEmptyRoomId() {
        assertThatThrownBy(() -> RoomAggregate.create("", "host123", strategy))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Room ID cannot be null");
    }

    @Test
    @DisplayName("Не должен позволить создать комнату с null host ID")
    void shouldNotAllowNullHostId() {
        assertThatThrownBy(() -> RoomAggregate.create("room1", null, strategy))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Host ID cannot be null");
    }

    @Test
    @DisplayName("Не должен позволить создать комнату с null стратегией")
    void shouldNotAllowNullStrategy() {
        assertThatThrownBy(() -> RoomAggregate.create("room1", "host123", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Completion strategy cannot be null");
    }
}
