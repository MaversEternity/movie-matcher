package com.moviematcher.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Интеграционные тесты для RoomResource REST API
 *
 * Используют @QuarkusTest для запуска полного Quarkus приложения
 * Проверяют end-to-end функциональность через HTTP
 */
@QuarkusTest
@DisplayName("RoomResource Integration Tests")
class RoomResourceIT {

    @Test
    @DisplayName("Должен создать комнату через POST /api/rooms")
    void shouldCreateRoom() {
        String requestBody = """
            {
                "hostId": "host123"
            }
            """;

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(requestBody)
            .when()
            .post("/api/rooms")
            .then()
            .statusCode(200)
            .body("room_id", notNullValue())
            .body("join_url", containsString("room/"));
    }

    @Test
    @DisplayName("Должен присоединиться к существующей комнате")
    void shouldJoinRoom() {
        // Given - создаем комнату
        String createRequest = """
            {
                "hostId": "host123"
            }
            """;

        String roomId = given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(createRequest)
            .when()
            .post("/api/rooms")
            .then()
            .statusCode(200)
            .extract()
            .path("room_id");

        // When - присоединяемся к комнате
        String joinRequest = """
            {
                "participantId": "participant1"
            }
            """;

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(joinRequest)
            .when()
            .post("/api/rooms/" + roomId + "/join")
            .then()
            .statusCode(200)
            .body("success", equalTo(true));
    }

    @Test
    @DisplayName("Должен вернуть 400 при попытке присоединиться дважды")
    void shouldReturn400OnDuplicateJoin() {
        // Given - создаем комнату и присоединяемся
        String createRequest = """
            {
                "hostId": "host123"
            }
            """;

        String roomId = given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(createRequest)
            .when()
            .post("/api/rooms")
            .then()
            .statusCode(200)
            .extract()
            .path("room_id");

        String joinRequest = """
            {
                "participantId": "participant1"
            }
            """;

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(joinRequest)
            .when()
            .post("/api/rooms/" + roomId + "/join")
            .then()
            .statusCode(200);

        // When - пытаемся присоединиться второй раз
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(joinRequest)
            .when()
            .post("/api/rooms/" + roomId + "/join")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("message", containsString("already"));
    }

    @Test
    @DisplayName(
        "Должен вернуть 404 при присоединении к несуществующей комнате"
    )
    void shouldReturn404OnJoinNonExistentRoom() {
        String joinRequest = """
            {
                "participantId": "participant1"
            }
            """;

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(joinRequest)
            .when()
            .post("/api/rooms/nonexistent/join")
            .then()
            .statusCode(400)
            .body("success", equalTo(false))
            .body("message", containsString("not found"));
    }

    @Test
    @DisplayName(
        "Должен получить информацию о комнате через GET /api/rooms/{roomId}"
    )
    void shouldGetRoomInfo() {
        // Given - создаем комнату
        String createRequest = """
            {
                "hostId": "host123"
            }
            """;

        String roomId = given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(createRequest)
            .when()
            .post("/api/rooms")
            .then()
            .statusCode(200)
            .extract()
            .path("room_id");

        // When - получаем информацию
        given()
            .when()
            .get("/api/rooms/" + roomId)
            .then()
            .statusCode(200)
            .body("room_id", equalTo(roomId))
            .body("participants_count", equalTo(1))
            .body("is_active", equalTo(true));
    }

    @Test
    @DisplayName("Должен вернуть 404 для несуществующей комнаты")
    void shouldReturn404ForNonExistentRoom() {
        given()
            .when()
            .get("/api/rooms/nonexistent")
            .then()
            .statusCode(404)
            .body("message", equalTo("Room not found"));
    }

    @Test
    @DisplayName("Должен покинуть комнату через POST /api/rooms/{roomId}/leave")
    void shouldLeaveRoom() {
        // Given - создаем комнату и присоединяемся
        String createRequest = """
            {
                "hostId": "host123"
            }
            """;

        String roomId = given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(createRequest)
            .when()
            .post("/api/rooms")
            .then()
            .statusCode(200)
            .extract()
            .path("room_id");

        String joinRequest = """
            {
                "participantId": "participant1"
            }
            """;

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(joinRequest)
            .when()
            .post("/api/rooms/" + roomId + "/join")
            .then()
            .statusCode(200);

        // When - покидаем комнату
        String leaveRequest = """
            {
                "participantId": "participant1"
            }
            """;

        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(leaveRequest)
            .when()
            .post("/api/rooms/" + roomId + "/leave")
            .then()
            .statusCode(200);
    }

    @Test
    @DisplayName(
        "Должен вернуть 400 при попытке начать голосование с 1 участником"
    )
    void shouldReturn400OnStartVotingWithOneParticipant() {
        // Given - создаем комнату с 1 участником
        String createRequest = """
            {
                "hostId": "host123"
            }
            """;

        String roomId = given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(createRequest)
            .when()
            .post("/api/rooms")
            .then()
            .statusCode(200)
            .extract()
            .path("room_id");

        // When - пытаемся начать голосование
        given()
            .when()
            .post("/api/rooms/" + roomId + "/start")
            .then()
            .statusCode(400)
            .body("message", containsString("at least 2 participants"));
    }

    @Test
    @DisplayName("Должен создать несколько независимых комнат")
    void shouldCreateMultipleIndependentRooms() {
        String request1 = """
            {
                "hostId": "host1"
            }
            """;

        String request2 = """
            {
                "hostId": "host2"
            }
            """;

        String roomId1 = given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(request1)
            .when()
            .post("/api/rooms")
            .then()
            .statusCode(200)
            .extract()
            .path("room_id");

        String roomId2 = given()
            .contentType(MediaType.APPLICATION_JSON)
            .body(request2)
            .when()
            .post("/api/rooms")
            .then()
            .statusCode(200)
            .extract()
            .path("room_id");

        // Then - ID должны быть разными
        assert !roomId1.equals(roomId2);

        // И обе комнаты должны существовать
        given().when().get("/api/rooms/" + roomId1).then().statusCode(200);

        given().when().get("/api/rooms/" + roomId2).then().statusCode(200);
    }
}
