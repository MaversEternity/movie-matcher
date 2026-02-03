package com.moviematcher.infrastructure.chain;

import static org.assertj.core.api.Assertions.*;

import com.moviematcher.entity.Movie;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke тесты для Chain of Responsibility Pattern
 *
 * Проверяют:
 * - Построение цепочки обработчиков
 * - Передачу запроса по цепочке
 * - Поиск в разных источниках
 */
@DisplayName("MovieSearchHandler Chain of Responsibility Tests")
class MovieSearchHandlerTest {

    /**
     * Mock обработчик для тестирования
     */
    static class MockSearchHandler extends MovieSearchHandler {

        private final String name;
        private final boolean shouldFind;
        private final String movieTitle;

        public MockSearchHandler(
            String name,
            boolean shouldFind,
            String movieTitle
        ) {
            this.name = name;
            this.shouldFind = shouldFind;
            this.movieTitle = movieTitle;
        }

        @Override
        public Optional<Movie> search(String query) {
            if (shouldFind && query.equals(movieTitle)) {
                Movie movie = new Movie();
                movie.title = movieTitle;
                movie.imdbId = "tt123";
                return Optional.of(movie);
            }
            return searchNext(query);
        }

        @Override
        public String getHandlerName() {
            return name;
        }
    }

    @Test
    @DisplayName("Должен найти в первом обработчике цепочки")
    void shouldFindInFirstHandler() {
        // Given - цепочка: DB → TMDB → OMDB
        MovieSearchHandler db = new MockSearchHandler("DB", true, "Inception");
        MovieSearchHandler tmdb = new MockSearchHandler("TMDB", false, null);
        MovieSearchHandler omdb = new MockSearchHandler("OMDB", false, null);

        db.setNext(tmdb);
        tmdb.setNext(omdb);

        // When
        Optional<Movie> result = db.search("Inception");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().title).isEqualTo("Inception");
    }

    @Test
    @DisplayName(
        "Должен передать поиск во второй обработчик если первый не нашел"
    )
    void shouldPassToSecondHandlerIfFirstNotFound() {
        // Given
        MovieSearchHandler db = new MockSearchHandler("DB", false, null);
        MovieSearchHandler tmdb = new MockSearchHandler(
            "TMDB",
            true,
            "Inception"
        );
        MovieSearchHandler omdb = new MockSearchHandler("OMDB", false, null);

        db.setNext(tmdb);
        tmdb.setNext(omdb);

        // When
        Optional<Movie> result = db.search("Inception");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().title).isEqualTo("Inception");
    }

    @Test
    @DisplayName("Должен передать поиск до конца цепочки")
    void shouldPassToLastHandler() {
        // Given
        MovieSearchHandler db = new MockSearchHandler("DB", false, null);
        MovieSearchHandler tmdb = new MockSearchHandler("TMDB", false, null);
        MovieSearchHandler omdb = new MockSearchHandler(
            "OMDB",
            true,
            "Inception"
        );

        db.setNext(tmdb);
        tmdb.setNext(omdb);

        // When
        Optional<Movie> result = db.search("Inception");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().title).isEqualTo("Inception");
    }

    @Test
    @DisplayName("Должен вернуть empty если никто не нашел")
    void shouldReturnEmptyIfNotFoundInChain() {
        // Given
        MovieSearchHandler db = new MockSearchHandler("DB", false, null);
        MovieSearchHandler tmdb = new MockSearchHandler("TMDB", false, null);
        MovieSearchHandler omdb = new MockSearchHandler("OMDB", false, null);

        db.setNext(tmdb);
        tmdb.setNext(omdb);

        // When
        Optional<Movie> result = db.search("NonExistentMovie");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Должен работать с одним обработчиком")
    void shouldWorkWithSingleHandler() {
        // Given
        MovieSearchHandler db = new MockSearchHandler("DB", true, "Inception");

        // When
        Optional<Movie> result = db.search("Inception");

        // Then
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Должен построить цепочку через setNext fluent API")
    void shouldBuildChainUsingFluentApi() {
        // Given
        MovieSearchHandler db = new MockSearchHandler("DB", false, null);
        MovieSearchHandler tmdb = new MockSearchHandler("TMDB", false, null);
        MovieSearchHandler omdb = new MockSearchHandler(
            "OMDB",
            true,
            "Inception"
        );

        // When - fluent API
        db.setNext(tmdb).setNext(omdb);

        Optional<Movie> result = db.search("Inception");

        // Then
        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("Должен вернуть имя обработчика")
    void shouldReturnHandlerName() {
        // Given
        MovieSearchHandler handler = new MockSearchHandler(
            "TestHandler",
            false,
            null
        );

        // When
        String name = handler.getHandlerName();

        // Then
        assertThat(name).isEqualTo("TestHandler");
    }
}
