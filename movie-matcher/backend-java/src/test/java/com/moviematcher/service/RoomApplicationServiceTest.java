package com.moviematcher.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.moviematcher.domain.model.VotingCompletionType;
import com.moviematcher.model.CreateRoomResponse;
import com.moviematcher.model.JoinRoomResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Smoke тесты для RoomApplicationService
 *
 * Проверяют базовую функциональность application сервиса:
 * - Создание комнаты
 * - Присоединение к комнате
 * - Управление жизненным циклом
 */
@DisplayName("RoomApplicationService Smoke Tests")
class RoomApplicationServiceTest {

    @Mock
    private WebSocketBroadcastService broadcastService;

    @Mock
    private MovieSelectionService movieSelectionService;

    private RoomApplicationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new RoomApplicationService(
            broadcastService,
            movieSelectionService
        );
    }

    @Test
    @DisplayName("Должен создать комнату с единогласным голосованием")
    void shouldCreateRoomWithUnanimousVoting() {
        // Given
        String hostId = "host123";
        VotingCompletionType completionType = VotingCompletionType.UNANIMOUS;

        // When
        CreateRoomResponse response = service.createRoom(
            hostId,
            completionType
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.roomId()).isNotNull();
        assertThat(response.roomId()).isNotEmpty();
        assertThat(response.joinUrl()).contains(response.roomId());
    }

    @Test
    @DisplayName("Должен создать комнату с голосованием большинством")
    void shouldCreateRoomWithMajorityVoting() {
        // Given
        String hostId = "host123";
        VotingCompletionType completionType = VotingCompletionType.MAJORITY;

        // When
        CreateRoomResponse response = service.createRoom(
            hostId,
            completionType
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.roomId()).isNotNull();
    }

    @Test
    @DisplayName("Должен присоединить участника к существующей комнате")
    void shouldJoinExistingRoom() {
        // Given
        String hostId = "host123";
        CreateRoomResponse createResponse = service.createRoom(
            hostId,
            VotingCompletionType.UNANIMOUS
        );
        String roomId = createResponse.roomId();

        // When
        JoinRoomResponse joinResponse = service.joinRoom(
            roomId,
            "participant1"
        );

        // Then
        assertThat(joinResponse.success()).isTrue();
        verify(broadcastService).broadcast(eq(roomId), any());
    }

    @Test
    @DisplayName("Не должен присоединить к несуществующей комнате")
    void shouldNotJoinNonExistentRoom() {
        // When
        JoinRoomResponse response = service.joinRoom(
            "nonexistent",
            "participant1"
        );

        // Then
        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("not found");
    }

    @Test
    @DisplayName("Не должен позволить присоединиться дважды")
    void shouldNotAllowDuplicateJoin() {
        // Given
        String hostId = "host123";
        CreateRoomResponse createResponse = service.createRoom(
            hostId,
            VotingCompletionType.UNANIMOUS
        );
        String roomId = createResponse.roomId();
        service.joinRoom(roomId, "participant1");

        // When
        JoinRoomResponse response = service.joinRoom(roomId, "participant1");

        // Then
        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("already");
    }

    @Test
    @DisplayName("Должен создавать уникальные ID для каждой комнаты")
    void shouldCreateUniqueRoomIds() {
        // When
        CreateRoomResponse room1 = service.createRoom(
            "host1",
            VotingCompletionType.UNANIMOUS
        );
        CreateRoomResponse room2 = service.createRoom(
            "host2",
            VotingCompletionType.UNANIMOUS
        );

        // Then
        assertThat(room1.roomId()).isNotEqualTo(room2.roomId());
    }
}
