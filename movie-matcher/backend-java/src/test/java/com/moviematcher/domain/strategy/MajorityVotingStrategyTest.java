package com.moviematcher.domain.strategy;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke тесты для MajorityVotingStrategy
 *
 * Проверяют:
 * - Голосование большинством (70% участников)
 * - Определение совпадений
 * - Округление вверх для требуемых голосов
 */
@DisplayName("MajorityVotingStrategy Smoke Tests")
class MajorityVotingStrategyTest {

    private MajorityVotingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new MajorityVotingStrategy();
    }

    @Test
    @DisplayName("Должен завершить при 70% голосов (3 из 4)")
    void shouldCompleteWith70PercentVotes() {
        // Given - 3 из 4 участников лайкнули movie1 (75%)
        Map<String, Set<String>> likes = Map.of(
            "user1",
            Set.of("movie1"),
            "user2",
            Set.of("movie1"),
            "user3",
            Set.of("movie1"),
            "user4",
            Set.of("movie2")
        );

        // When
        boolean isComplete = strategy.isComplete(likes, 4);

        // Then
        assertThat(isComplete).isTrue();
    }

    @Test
    @DisplayName("Не должен завершить при менее 70% голосов")
    void shouldNotCompleteWithLessThan70Percent() {
        // Given - только 2 из 4 участников лайкнули movie1 (50%)
        Map<String, Set<String>> likes = Map.of(
            "user1",
            Set.of("movie1"),
            "user2",
            Set.of("movie1"),
            "user3",
            Set.of("movie2"),
            "user4",
            Set.of("movie3")
        );

        // When
        boolean isComplete = strategy.isComplete(likes, 4);

        // Then
        assertThat(isComplete).isFalse();
    }

    @Test
    @DisplayName("Должен округлять вверх необходимое количество голосов")
    void shouldCeilRequiredVotes() {
        // Given - 3 участника, требуется 70% = 2.1 → округляется до 3
        // Только 2 лайкнули movie1 - недостаточно
        Map<String, Set<String>> likes = Map.of(
            "user1",
            Set.of("movie1"),
            "user2",
            Set.of("movie1"),
            "user3",
            Set.of("movie2")
        );

        // When
        boolean isComplete = strategy.isComplete(likes, 3);

        // Then
        assertThat(isComplete).isFalse(); // Нужно 3, а есть только 2
    }

    @Test
    @DisplayName("Должен завершить когда все 3 лайкнули (100% > 70%)")
    void shouldCompleteWhenAll3Liked() {
        // Given - все 3 лайкнули movie1
        Map<String, Set<String>> likes = Map.of(
            "user1",
            Set.of("movie1"),
            "user2",
            Set.of("movie1"),
            "user3",
            Set.of("movie1")
        );

        // When
        boolean isComplete = strategy.isComplete(likes, 3);

        // Then
        assertThat(isComplete).isTrue();
    }

    @Test
    @DisplayName("Должен вернуть совпавшие фильмы с большинством голосов")
    void shouldReturnMatchedMoviesWithMajority() {
        // Given - movie1: 3 голоса (75%), movie2: 2 голоса (50%)
        Map<String, Set<String>> likes = Map.of(
            "user1",
            Set.of("movie1", "movie2"),
            "user2",
            Set.of("movie1", "movie2"),
            "user3",
            Set.of("movie1"),
            "user4",
            Set.of("movie3")
        );

        // When
        List<String> matched = strategy.getMatchedMovies(likes, 4);

        // Then
        assertThat(matched).containsExactly("movie1"); // Только movie1 >= 70%
    }

    @Test
    @DisplayName("Должен вернуть несколько фильмов если у всех большинство")
    void shouldReturnMultipleMoviesIfAllHaveMajority() {
        // Given - и movie1, и movie2 набрали 3 голоса из 4 (75%)
        Map<String, Set<String>> likes = Map.of(
            "user1",
            Set.of("movie1", "movie2"),
            "user2",
            Set.of("movie1", "movie2"),
            "user3",
            Set.of("movie1", "movie2"),
            "user4",
            Set.of("movie3")
        );

        // When
        List<String> matched = strategy.getMatchedMovies(likes, 4);

        // Then
        assertThat(matched).containsExactlyInAnyOrder("movie1", "movie2");
    }

    @Test
    @DisplayName("Должен вернуть пустой список если нет фильма с большинством")
    void shouldReturnEmptyListIfNoMajority() {
        // Given
        Map<String, Set<String>> likes = Map.of(
            "user1",
            Set.of("movie1"),
            "user2",
            Set.of("movie2"),
            "user3",
            Set.of("movie3"),
            "user4",
            Set.of("movie4")
        );

        // When
        List<String> matched = strategy.getMatchedMovies(likes, 4);

        // Then
        assertThat(matched).isEmpty();
    }

    @Test
    @DisplayName("Должен работать с 2 участниками (70% = 2)")
    void shouldWorkWithTwoParticipants() {
        // Given - оба лайкнули movie1
        Map<String, Set<String>> likes = Map.of(
            "user1",
            Set.of("movie1"),
            "user2",
            Set.of("movie1")
        );

        // When
        boolean isComplete = strategy.isComplete(likes, 2);
        List<String> matched = strategy.getMatchedMovies(likes, 2);

        // Then
        assertThat(isComplete).isTrue();
        assertThat(matched).containsExactly("movie1");
    }

    @Test
    @DisplayName("Должен обработать пустые лайки")
    void shouldHandleEmptyLikes() {
        // Given
        Map<String, Set<String>> likes = Map.of();

        // When
        boolean isComplete = strategy.isComplete(likes, 4);
        List<String> matched = strategy.getMatchedMovies(likes, 4);

        // Then
        assertThat(isComplete).isFalse();
        assertThat(matched).isEmpty();
    }

    @Test
    @DisplayName("Должен работать с 10 участниками (70% = 7)")
    void shouldWorkWith10Participants() {
        // Given - 7 из 10 лайкнули movie1
        Map<String, Set<String>> likes = new HashMap<>();
        for (int i = 1; i <= 7; i++) {
            likes.put("user" + i, Set.of("movie1"));
        }
        for (int i = 8; i <= 10; i++) {
            likes.put("user" + i, Set.of("movie2"));
        }

        // When
        boolean isComplete = strategy.isComplete(likes, 10);
        List<String> matched = strategy.getMatchedMovies(likes, 10);

        // Then
        assertThat(isComplete).isTrue();
        assertThat(matched).containsExactly("movie1");
    }
}
