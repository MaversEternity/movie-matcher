package com.moviematcher.domain.model;

import static org.assertj.core.api.Assertions.*;

import com.moviematcher.domain.strategy.UnanimousVotingStrategy;
import com.moviematcher.domain.strategy.VotingCompletionStrategy;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke тесты для VotingSession
 *
 * Проверяют:
 * - Круговую систему голосования (Round-Robin)
 * - Приоритет вручную выбранных фильмов
 * - Уникальность показываемых фильмов
 * - Логику завершения голосования
 */
@DisplayName("VotingSession Smoke Tests")
class VotingSessionTest {

    private Participant participant1;
    private Participant participant2;
    private VotingCompletionStrategy strategy;

    @BeforeEach
    void setUp() {
        participant1 = new Participant("user1", true);
        participant2 = new Participant("user2", false);
        strategy = new UnanimousVotingStrategy();
    }

    @Test
    @DisplayName("Должен создать сессию голосования")
    void shouldCreateVotingSession() {
        // Given
        List<Participant> participants = Arrays.asList(
            participant1,
            participant2
        );

        // When
        VotingSession session = new VotingSession(participants, strategy);

        // Then
        assertThat(session).isNotNull();
        assertThat(session.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("Не должен создать сессию с менее чем 2 участниками")
    void shouldNotCreateSessionWithLessThan2Participants() {
        // Given
        List<Participant> participants = List.of(participant1);

        // When / Then
        assertThatThrownBy(() -> new VotingSession(participants, strategy))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least 2 participants");
    }

    @Test
    @DisplayName("Должен вернуть вручную выбранный фильм первым")
    void shouldReturnManualMovieFirst() {
        // Given
        participant1.addManuallySelectedMovie("manual1");
        participant2.setFilters(
            new MovieFilters(null, null, null, null, null)
        );

        List<Participant> participants = Arrays.asList(
            participant1,
            participant2
        );
        VotingSession session = new VotingSession(participants, strategy);

        // Добавляем фильмы из фильтров
        session.addMoviesToParticipant("user1", List.of("auto1", "auto2"));

        // When
        Optional<String> nextMovie = session.getNextMovie();

        // Then
        assertThat(nextMovie).contains("manual1"); // Вручную выбранный первым!
    }

    @Test
    @DisplayName("Должен чередовать фильмы между участниками")
    void shouldRotateMoviesBetweenParticipants() {
        // Given
        List<Participant> participants = Arrays.asList(
            participant1,
            participant2
        );
        VotingSession session = new VotingSession(participants, strategy);

        session.addMoviesToParticipant("user1", List.of("movie1", "movie2"));
        session.addMoviesToParticipant("user2", List.of("movie3", "movie4"));

        // When
        String first = session.getNextMovie().orElse(null);
        String second = session.getNextMovie().orElse(null);
        String third = session.getNextMovie().orElse(null);

        // Then - чередование Round-Robin
        assertThat(first).isEqualTo("movie1"); // user1
        assertThat(second).isEqualTo("movie3"); // user2
        assertThat(third).isEqualTo("movie2"); // user1 снова
    }

    @Test
    @DisplayName("Не должен показывать один и тот же фильм дважды")
    void shouldNotShowSameMovieTwice() {
        // Given
        List<Participant> participants = Arrays.asList(
            participant1,
            participant2
        );
        VotingSession session = new VotingSession(participants, strategy);

        session.addMoviesToParticipant("user1", List.of("movie1"));
        session.addMoviesToParticipant("user2", List.of("movie1")); // Дубликат!

        // When
        String first = session.getNextMovie().orElse(null);
        String second = session.getNextMovie().orElse(null);

        // Then
        assertThat(first).isEqualTo("movie1");
        assertThat(second).isNull(); // movie1 уже был показан
    }

    @Test
    @DisplayName("Должен записать голос участника")
    void shouldRecordVote() {
        // Given
        List<Participant> participants = Arrays.asList(
            participant1,
            participant2
        );
        VotingSession session = new VotingSession(participants, strategy);
        session.addMoviesToParticipant("user1", List.of("movie1"));

        // When
        session.recordVote("user1", "movie1", true);

        // Then
        assertThat(participant1.hasLiked("movie1")).isTrue();
    }

    @Test
    @DisplayName("Должен завершить голосование при единогласном лайке")
    void shouldCompleteVotingOnUnanimousLike() {
        // Given
        List<Participant> participants = Arrays.asList(
            participant1,
            participant2
        );
        VotingSession session = new VotingSession(participants, strategy);

        // When - оба участника лайкают один и тот же фильм
        session.recordVote("user1", "movie1", true);
        session.recordVote("user2", "movie1", true);

        // Then
        assertThat(session.isCompleted()).isTrue();
        assertThat(session.getMatchedMovies()).containsExactly("movie1");
    }

    @Test
    @DisplayName("Не должен завершить голосование если не все лайкнули")
    void shouldNotCompleteIfNotUnanimous() {
        // Given
        List<Participant> participants = Arrays.asList(
            participant1,
            participant2
        );
        VotingSession session = new VotingSession(participants, strategy);

        // When - только один лайкнул
        session.recordVote("user1", "movie1", true);
        session.recordVote("user2", "movie1", false); // Дизлайк!

        // Then
        assertThat(session.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("Должен проверить показан ли фильм")
    void shouldCheckIfMovieShown() {
        // Given
        List<Participant> participants = Arrays.asList(
            participant1,
            participant2
        );
        VotingSession session = new VotingSession(participants, strategy);
        session.addMoviesToParticipant("user1", List.of("movie1"));

        // When
        session.getNextMovie(); // Показываем movie1

        // Then
        assertThat(session.isMovieShown("movie1")).isTrue();
        assertThat(session.isMovieShown("movie2")).isFalse();
    }

    @Test
    @DisplayName("Должен вернуть empty когда фильмы закончились")
    void shouldReturnEmptyWhenNoMoreMovies() {
        // Given
        List<Participant> participants = Arrays.asList(
            participant1,
            participant2
        );
        VotingSession session = new VotingSession(participants, strategy);

        // When - нет добавленных фильмов
        Optional<String> nextMovie = session.getNextMovie();

        // Then
        assertThat(nextMovie).isEmpty();
    }

    @Test
    @DisplayName("Должен вернуть все лайки участников")
    void shouldReturnAllLikes() {
        // Given
        List<Participant> participants = Arrays.asList(
            participant1,
            participant2
        );
        VotingSession session = new VotingSession(participants, strategy);

        session.recordVote("user1", "movie1", true);
        session.recordVote("user1", "movie2", true);
        session.recordVote("user2", "movie1", true);

        // When
        var allLikes = session.getAllLikes();

        // Then
        assertThat(allLikes.get("user1")).containsExactlyInAnyOrder(
            "movie1",
            "movie2"
        );
        assertThat(allLikes.get("user2")).containsExactly("movie1");
    }

    @Test
    @DisplayName("Не должен записать голос несуществующего участника")
    void shouldNotRecordVoteForNonExistentParticipant() {
        // Given
        List<Participant> participants = Arrays.asList(
            participant1,
            participant2
        );
        VotingSession session = new VotingSession(participants, strategy);

        // When / Then
        assertThatThrownBy(() ->
                session.recordVote("nonexistent", "movie1", true)
            )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Participant not found");
    }

    @Test
    @DisplayName("Должен добавить фильмы участнику во время голосования")
    void shouldAddMoviesToParticipantDuringVoting() {
        // Given
        List<Participant> participants = Arrays.asList(
            participant1,
            participant2
        );
        VotingSession session = new VotingSession(participants, strategy);

        // When
        session.addMoviesToParticipant("user1", List.of("movie1", "movie2"));

        // Then
        Optional<String> first = session.getNextMovie();
        Optional<String> second = session.getNextMovie();

        assertThat(first).contains("movie1");
        assertThat(second).isPresent();
    }

    @Test
    @DisplayName("Не должен добавить дубликаты фильмов")
    void shouldNotAddDuplicateMovies() {
        // Given
        List<Participant> participants = Arrays.asList(
            participant1,
            participant2
        );
        VotingSession session = new VotingSession(participants, strategy);

        session.addMoviesToParticipant("user1", List.of("movie1"));
        session.getNextMovie(); // Показываем movie1

        // When
        session.addMoviesToParticipant("user1", List.of("movie1", "movie2")); // movie1 дубликат

        // Then
        Optional<String> next = session.getNextMovie();
        assertThat(next).contains("movie2"); // movie1 пропущен как дубликат
    }
}
