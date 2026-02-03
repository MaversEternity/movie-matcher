package com.moviematcher.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke тесты для Participant entity
 *
 * Проверяют:
 * - Управление фильтрами
 * - Добавление фильмов вручную
 * - Лайки и дизлайки
 * - Готовность к голосованию
 */
@DisplayName("Participant Smoke Tests")
class ParticipantTest {

    @Test
    @DisplayName("Должен создать участника")
    void shouldCreateParticipant() {
        // When
        Participant participant = new Participant("user1", false);

        // Then
        assertThat(participant.getId()).isEqualTo("user1");
        assertThat(participant.isReadyToVote()).isFalse();
    }

    @Test
    @DisplayName("Должен установить фильтры")
    void shouldSetFilters() {
        // Given
        Participant participant = new Participant("user1", false);
        MovieFilters filters = new MovieFilters(
            "Action",
            2020,
            2023,
            java.math.BigDecimal.valueOf(7.0),
            "movie"
        );

        // When
        participant.setFilters(filters);

        // Then
        assertThat(participant.getFilters()).isEqualTo(filters);
    }

    @Test
    @DisplayName("Не должен позволить установить null фильтры")
    void shouldNotAllowNullFilters() {
        // Given
        Participant participant = new Participant("user1", false);

        // When / Then
        assertThatThrownBy(() -> participant.setFilters(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Filters cannot be null");
    }

    @Test
    @DisplayName("Должен добавить фильм вручную")
    void shouldAddManualMovie() {
        // Given
        Participant participant = new Participant("user1", false);

        // When
        participant.addManuallySelectedMovie("movie1");

        // Then
        assertThat(participant.hasManualMovies()).isTrue();
    }

    @Test
    @DisplayName("Должен получить вручную выбранный фильм")
    void shouldPollManualMovie() {
        // Given
        Participant participant = new Participant("user1", false);
        participant.addManuallySelectedMovie("movie1");
        participant.addManuallySelectedMovie("movie2");

        // When
        Optional<String> first = participant.pollManualMovie();
        Optional<String> second = participant.pollManualMovie();

        // Then
        assertThat(first).contains("movie1");
        assertThat(second).contains("movie2");
        assertThat(participant.hasManualMovies()).isFalse();
    }

    @Test
    @DisplayName("Не должен позволить добавить null фильм")
    void shouldNotAllowNullMovie() {
        // Given
        Participant participant = new Participant("user1", false);

        // When / Then
        assertThatThrownBy(() -> participant.addManuallySelectedMovie(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Movie ID cannot be null");
    }

    @Test
    @DisplayName("Не должен позволить добавить пустой ID фильма")
    void shouldNotAllowEmptyMovieId() {
        // Given
        Participant participant = new Participant("user1", false);

        // When / Then
        assertThatThrownBy(() -> participant.addManuallySelectedMovie(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Movie ID cannot be null");
    }

    @Test
    @DisplayName("Должен лайкнуть фильм")
    void shouldLikeMovie() {
        // Given
        Participant participant = new Participant("user1", false);

        // When
        participant.likeMovie("movie1");

        // Then
        assertThat(participant.hasLiked("movie1")).isTrue();
        assertThat(participant.getLikedMovies()).containsExactly("movie1");
    }

    @Test
    @DisplayName("Должен дизлайкнуть фильм")
    void shouldDislikeMovie() {
        // Given
        Participant participant = new Participant("user1", false);

        // When
        participant.dislikeMovie("movie1");

        // Then
        assertThat(participant.hasLiked("movie1")).isFalse();
        assertThat(participant.getLikedMovies()).isEmpty();
    }

    @Test
    @DisplayName("Должен убрать из дизлайков при лайке")
    void shouldRemoveFromDislikesWhenLiking() {
        // Given
        Participant participant = new Participant("user1", false);
        participant.dislikeMovie("movie1");

        // When
        participant.likeMovie("movie1");

        // Then
        assertThat(participant.hasLiked("movie1")).isTrue();
    }

    @Test
    @DisplayName("Должен убрать из лайков при дизлайке")
    void shouldRemoveFromLikesWhenDisliking() {
        // Given
        Participant participant = new Participant("user1", false);
        participant.likeMovie("movie1");

        // When
        participant.dislikeMovie("movie1");

        // Then
        assertThat(participant.hasLiked("movie1")).isFalse();
    }

    @Test
    @DisplayName("Должен пометить готовым к голосованию с фильтрами")
    void shouldMarkReadyWithFilters() {
        // Given
        Participant participant = new Participant("user1", false);
        participant.setFilters(
            new MovieFilters(null, null, null, null, null)
        );

        // When
        participant.markReady();

        // Then
        assertThat(participant.isReadyToVote()).isTrue();
    }

    @Test
    @DisplayName("Должен пометить готовым к голосованию с вручную выбранными фильмами")
    void shouldMarkReadyWithManualMovies() {
        // Given
        Participant participant = new Participant("user1", false);
        participant.addManuallySelectedMovie("movie1");

        // When
        participant.markReady();

        // Then
        assertThat(participant.isReadyToVote()).isTrue();
    }

    @Test
    @DisplayName("Не должен позволить пометить готовым без фильтров и фильмов")
    void shouldNotMarkReadyWithoutFiltersOrMovies() {
        // Given
        Participant participant = new Participant("user1", false);

        // When / Then
        assertThatThrownBy(() -> participant.markReady())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must set filters or add movies");
    }

    @Test
    @DisplayName("Должен вернуть defensive copy лайков")
    void shouldReturnDefensiveCopyOfLikes() {
        // Given
        Participant participant = new Participant("user1", false);
        participant.likeMovie("movie1");

        // When
        var likes = participant.getLikedMovies();
        likes.add("movie2"); // Пытаемся изменить копию

        // Then
        assertThat(participant.getLikedMovies()).containsExactly("movie1"); // Оригинал не изменен
    }

    @Test
    @DisplayName("Должен корректно работать equals и hashCode")
    void shouldWorkWithEqualsAndHashCode() {
        // Given
        Participant p1 = new Participant("user1", false);
        Participant p2 = new Participant("user1", true);
        Participant p3 = new Participant("user2", false);

        // Then
        assertThat(p1).isEqualTo(p2); // Одинаковый ID
        assertThat(p1).isNotEqualTo(p3); // Разный ID
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }
}
