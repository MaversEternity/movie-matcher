package com.moviematcher.websocket;

import static org.assertj.core.api.Assertions.*;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.websocket.*;
import java.net.URI;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Интеграционные тесты для RoomWebSocket
 *
 * WebSocket тесты требуют более сложной настройки клиента
 * Эти тесты помечены как @Disabled для первоначального запуска
 * TODO: Реализовать полную WebSocket тестовую инфраструктуру
 */
@QuarkusTest
@DisplayName("RoomWebSocket Integration Tests")
@Disabled("WebSocket tests require additional setup - implement when needed")
class RoomWebSocketIT {

    @TestHTTPResource("/ws/room")
    URI wsUri;

    /**
     * Простой WebSocket клиент для тестирования
     */
    @ClientEndpoint
    public static class TestClient {

        private final LinkedBlockingDeque<String> messages = new LinkedBlockingDeque<>();
        private Session session;

        @OnOpen
        public void onOpen(Session session) {
            this.session = session;
        }

        @OnMessage
        public void onMessage(String message) {
            messages.add(message);
        }

        public void sendMessage(String message) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                throw new RuntimeException("Failed to send message", e);
            }
        }

        public String waitForMessage() throws InterruptedException {
            return messages.poll(10, TimeUnit.SECONDS);
        }

        public void close() {
            try {
                if (session != null) {
                    session.close();
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    @DisplayName("Должен подключиться к WebSocket")
    void shouldConnectToWebSocket() throws Exception {
        // Given
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        TestClient client = new TestClient();

        // When
        Session session = container.connectToServer(client, wsUri);

        // Then
        assertThat(session.isOpen()).isTrue();

        // Cleanup
        session.close();
    }

    @Test
    @DisplayName("Должен получить подтверждение создания комнаты")
    void shouldReceiveRoomCreatedConfirmation() throws Exception {
        // Given
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        TestClient client = new TestClient();
        Session session = container.connectToServer(client, wsUri);

        // When - отправляем CREATE_ROOM
        String createRoomMessage = """
            {
                "type": "CREATE_ROOM",
                "hostId": "testHost"
            }
            """;
        client.sendMessage(createRoomMessage);

        // Then - должны получить ROOM_CREATED
        String response = client.waitForMessage();
        assertThat(response).contains("ROOM_CREATED");
        assertThat(response).contains("roomId");

        // Cleanup
        session.close();
    }

    @Test
    @DisplayName("Должен получить уведомление о присоединении участника")
    void shouldReceiveParticipantJoinedNotification() throws Exception {
        // Given - два клиента
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        TestClient host = new TestClient();
        TestClient participant = new TestClient();

        Session hostSession = container.connectToServer(host, wsUri);

        // Host создает комнату
        String createMessage = """
            {
                "type": "CREATE_ROOM",
                "hostId": "host123"
            }
            """;
        host.sendMessage(createMessage);
        String createResponse = host.waitForMessage();

        // Извлекаем roomId из ответа (упрощенная версия)
        // В реальном тесте нужен парсинг JSON
        String roomId = "extracted-room-id"; // TODO: parse from response

        // When - участник присоединяется
        Session participantSession = container.connectToServer(
            participant,
            wsUri
        );
        String joinMessage =
            """
            {
                "type": "JOIN_ROOM",
                "roomId": "%s",
                "participantId": "participant1"
            }
            """.formatted(roomId);
        participant.sendMessage(joinMessage);

        // Then - хост должен получить уведомление
        String notification = host.waitForMessage();
        assertThat(notification).contains("PARTICIPANT_JOINED");
        assertThat(notification).contains("participant1");

        // Cleanup
        hostSession.close();
        participantSession.close();
    }

    @Test
    @DisplayName("Должен обработать ошибку при присоединении к несуществующей комнате")
    void shouldHandleJoinNonExistentRoom() throws Exception {
        // Given
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        TestClient client = new TestClient();
        Session session = container.connectToServer(client, wsUri);

        // When - пытаемся присоединиться к несуществующей комнате
        String joinMessage = """
            {
                "type": "JOIN_ROOM",
                "roomId": "nonexistent",
                "participantId": "participant1"
            }
            """;
        client.sendMessage(joinMessage);

        // Then - должны получить сообщение об ошибке
        String response = client.waitForMessage();
        assertThat(response).contains("ERROR");
        assertThat(response).containsAnyOf("not found", "NOT_FOUND");

        // Cleanup
        session.close();
    }
}
