package com.moviematcher.domain.model;

import com.moviematcher.domain.strategy.VotingCompletionStrategy;
import java.util.*;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * Сессия голосования - Entity по DDD
 *
 * Инкапсулирует логику круговой системы голосования:
 * 1. Сначала показываются фильмы, добавленные участниками вручную
 * 2. Затем фильмы чередуются по участникам: User1 → User2 → User1 → User2
 * 3. Один раунд = показан 1 фильм от каждого участника
 * 4. Проверка совпадений после каждого раунда
 * 5. Уникальность - никакой фильм не показывается дважды
 */
public class VotingSession {

    private static final Logger log = Logger.getLogger(VotingSession.class);

    private final List<Participant> participants;
    private final VotingCompletionStrategy completionStrategy;

    // Текущий раунд голосования
    private int currentRound = 0;

    // Фильмы, показанные участникам (для уникальности)
    private final Set<String> shownMovieIds = new HashSet<>();

    // Очереди фильмов каждого участника (из фильтров)
    private final Map<String, Queue<String>> participantMovieQueues =
        new HashMap<>();

    // Индекс текущего участника для чередования
    private int currentParticipantIndex = 0;

    // Флаг завершения
    private boolean completed = false;

    // Найденные совпадения
    private List<String> matchedMovies = new ArrayList<>();

    public VotingSession(
        List<Participant> participants,
        VotingCompletionStrategy completionStrategy
    ) {
        if (participants == null || participants.size() < 2) {
            throw new IllegalArgumentException(
                "Need at least 2 participants for voting"
            );
        }

        this.participants = new ArrayList<>(participants);
        this.completionStrategy = completionStrategy;

        // Инициализируем очереди для каждого участника
        for (Participant participant : participants) {
            participantMovieQueues.put(participant.getId(), new LinkedList<>());
        }

        log.infof(
            "Created voting session with {} participants",
            participants.size()
        );
    }

    /**
     * Получить следующий фильм для показа
     *
     * Логика приоритета:
     * 1. Сначала все фильмы, добавленные вручную (в порядке добавления)
     * 2. Затем чередование фильмов из фильтров участников
     *
     * @return ID следующего фильма или empty если фильмы закончились
     */
    public Optional<String> getNextMovie() {
        // Приоритет 1: Вручную выбранные фильмы
        for (Participant participant : participants) {
            Optional<String> manualMovie = participant.pollManualMovie();
            if (manualMovie.isPresent() && !isMovieShown(manualMovie.get())) {
                markMovieAsShown(manualMovie.get());
                log.debugf(
                    "Next movie from manual selection: {}",
                    manualMovie.get()
                );
                return manualMovie;
            }
        }

        // Приоритет 2: Чередование по участникам (Round-Robin)
        String movieId = getNextMovieFromRotation();
        if (movieId != null) {
            markMovieAsShown(movieId);
            return Optional.of(movieId);
        }

        log.infof("No more movies available for voting");
        return Optional.empty();
    }

    /**
     * Чередование фильмов между участниками (Round-Robin)
     */
    private String getNextMovieFromRotation() {
        int attempts = 0;
        int maxAttempts = participants.size();

        while (attempts < maxAttempts) {
            Participant currentParticipant = participants.get(
                currentParticipantIndex
            );
            Queue<String> queue = participantMovieQueues.get(
                currentParticipant.getId()
            );

            // Пробуем взять фильм из очереди текущего участника
            String movieId = queue.poll();

            // Переходим к следующему участнику
            currentParticipantIndex =
                (currentParticipantIndex + 1) % participants.size();

            // Если взяли новый раунд (вернулись к первому участнику)
            if (currentParticipantIndex == 0) {
                currentRound++;
                log.debugf("Started round {}", currentRound);
            }

            if (movieId != null && !isMovieShown(movieId)) {
                log.debugf(
                    "Next movie from rotation: {} (participant: {}, round: {})",
                    movieId,
                    currentParticipant.getId(),
                    currentRound
                );
                return movieId;
            }

            attempts++;
        }

        return null; // Все очереди пусты
    }

    /**
     * Добавить фильмы в очередь участника
     * Вызывается когда загружаются фильмы по фильтрам
     */
    public void addMoviesToParticipant(
        String participantId,
        List<String> movieIds
    ) {
        Queue<String> queue = participantMovieQueues.get(participantId);
        if (queue == null) {
            log.warnf(
                "Participant {} not found in voting session",
                participantId
            );
            return;
        }

        // Добавляем только уникальные фильмы
        long added = movieIds
            .stream()
            .filter(id -> !isMovieShown(id))
            .peek(queue::offer)
            .count();

        log.debugf("Added {} movies to participant {}", added, participantId);
    }

    /**
     * Записать голос участника
     */
    public void recordVote(
        String participantId,
        String movieId,
        boolean isLike
    ) {
        Participant participant = findParticipant(participantId);
        if (participant == null) {
            throw new IllegalArgumentException(
                "Participant not found: " + participantId
            );
        }

        if (isLike) {
            participant.likeMovie(movieId);
        } else {
            participant.dislikeMovie(movieId);
        }

        log.debugf(
            "Recorded vote: participant={}, movie={}, like={}",
            participantId,
            movieId,
            isLike
        );

        // Проверяем условие завершения
        checkCompletion();
    }

    /**
     * Проверка условия завершения голосования
     */
    private void checkCompletion() {
        Map<String, Set<String>> participantLikes = participants
            .stream()
            .collect(
                Collectors.toMap(
                    Participant::getId,
                    Participant::getLikedMovies
                )
            );

        if (
            completionStrategy.isComplete(participantLikes, participants.size())
        ) {
            completed = true;
            matchedMovies = completionStrategy.getMatchedMovies(
                participantLikes,
                participants.size()
            );
            log.infof(
                "Voting completed! Matched {} movies",
                matchedMovies.size()
            );
        }
    }

    /**
     * Проверка, показан ли уже фильм
     */
    public boolean isMovieShown(String movieId) {
        return shownMovieIds.contains(movieId);
    }

    /**
     * Пометить фильм как показанный
     */
    private void markMovieAsShown(String movieId) {
        shownMovieIds.add(movieId);
    }

    /**
     * Найти участника по ID
     */
    private Participant findParticipant(String participantId) {
        return participants
            .stream()
            .filter(p -> p.getId().equals(participantId))
            .findFirst()
            .orElse(null);
    }

    /**
     * Получить все лайки всех участников
     */
    public Map<String, Set<String>> getAllLikes() {
        return participants
            .stream()
            .collect(
                Collectors.toMap(
                    Participant::getId,
                    Participant::getLikedMovies
                )
            );
    }

    /**
     * Проверить, завершено ли голосование
     */
    public boolean isCompleted() {
        return completed;
    }

    /**
     * Получить список совпавших фильмов
     */
    public List<String> getMatchedMovies() {
        return new ArrayList<>(matchedMovies);
    }
}
