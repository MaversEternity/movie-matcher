package com.moviematcher.domain.strategy;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke тесты для UnanimousVotingStrategy
 *
 * Проверяют:
 * - Единогласное голосование (100% участников)
 * - Определение совпадений
 */
@DisplayName("UnanimousVotingStrategy Smoke Tests")
class UnanimousVotingStrategyTest {

    private UnanimousVotingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new UnanimousVotingStrategy();
    }

    @Test
    @DisplayName("Должен завершить голосование при единогласном выборе")
    void shouldCompleteOnUnanimousVote() {
        // Given - все 3 участника лайкнули movie1
        Map<String, Set<String>> likes = Map.of(
            "user1",
            Set.of("movie1", "movie2"),
            "user2",
            Set.of("movie1", "movie3"),
            "user3",
            Set.of("movie1")
        );

        // When
        boolean isComplete = strategy.isComplete(likes, 3);

        // Then
        assertThat(isComplete).isTrue();
    }

    @Test
    @DisplayName("Не должен завершить если не все лайкнули один фильм")
    void shouldNotCompleteIfNotUnanimous() {
        // Given - нет фильма, который все лайкнули
        Map<String, Set<String>> likes = Map.of(
            "user1",
            Set.of("movie1", "movie2"),
            "user2",
            Set.of("movie2", "movie3"),
            "user3",
            Set.of("movie3", "movie1")
        );

        // When
        boolean isComplete = strategy.isComplete(likes, 3);

        // Then
        assertThat(isComplete).isFalse();
    }

    @Test
    @DisplayName("Должен вернуть совпавшие фильмы")
    void shouldReturnMatchedMovies() {
        // Given - все лайкнули movie1 и movie2
        Map<String, Set<String>> likes = Map.of(
            "user1",
            Set.of("movie1", "movie2", "movie3"),
            "user2",
            Set.of("movie1", "movie2", "movie4"),
            "user3",
            Set.of("movie1", "movie2")
        );

        // When
        List<String> matched = strategy.getMatchedMovies(likes, 3);

        // Then
        assertThat(matched).containsExactlyInAnyOrder("movie1", "movie2");
    }

    @Test
    @DisplayName("Должен вернуть пустой список если нет совпадений")
    void shouldReturnEmptyListIfNoMatches() {
        // Given
        Map<String, Set<String>> likes = Map.of(
            "user1",
            Set.of("movie1"),
            "user2",
            Set.of("movie2"),
            "user3",
            Set.of("movie3")
        );

        // When
        List<String> matched = strategy.getMatchedMovies(likes, 3);

        // Then
        assertThat(matched).isEmpty();
    }

    @Test
    @DisplayName("Не должен завершить если не все участники проголосовали")
    void shouldNotCompleteIfNotAllParticipantsVoted() {
        // Given - только 2 из 3 участников проголосовали
        Map<String, Set<String>> likes = Map.of(
            "user1",
            Set.of("movie1"),
            "user2",
            Set.of("movie1")
        );

        // When
        boolean isComplete = strategy.isComplete(likes, 3);

        // Then
        assertThat(isComplete).isFalse();
    }

    @Test
    @DisplayName("Должен работать с 2 участниками")
    void shouldWorkWithTwoParticipants() {
        // Given
        Map<String, Set<String>> likes = Map.of(
            "user1",
            Set.of("movie1", "movie2"),
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
        boolean isComplete = strategy.isComplete(likes, 2);
        List<String> matched = strategy.getMatchedMovies(likes, 2);

        // Then
        assertThat(isComplete).isFalse();
        assertThat(matched).isEmpty();
    }

    @Test
    @DisplayName("Должен обработать участника без лайков")
    void shouldHandleParticipantWithNoLikes() {
        // Given
        Map<String, Set<String>> likes = Map.of(
            "user1",
            Set.of("movie1"),
            "user2",
            Set.of() // Нет лайков
        );

        // When
        boolean isComplete = strategy.isComplete(likes, 2);

        // Then
        assertThat(isComplete).isFalse();
    }
}
