package com.moviematcher.resource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Интеграционные тесты для HealthResource
 *
 * Проверяют работу health check endpoints
 */
@QuarkusTest
@DisplayName("HealthResource Integration Tests")
class HealthResourceIT {

    @Test
    @DisplayName("Должен вернуть статус здоровья через GET /health")
    void shouldReturnHealthStatus() {
        given()
            .when()
            .get("/health")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("Должен вернуть liveness probe через GET /health/live")
    void shouldReturnLivenessProbe() {
        given()
            .when()
            .get("/health/live")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }

    @Test
    @DisplayName("Должен вернуть readiness probe через GET /health/ready")
    void shouldReturnReadinessProbe() {
        given()
            .when()
            .get("/health/ready")
            .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }
}
