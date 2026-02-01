package com.moviematcher.domain.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * Стратегия единогласного голосования
 *
 * Все участники (100%) должны лайкнуть один и тот же фильм для завершения голосования
 *
 * Single Responsibility: отвечает только за логику единогласного голосования
 */
@ApplicationScoped
public class UnanimousVotingStrategy implements VotingCompletionStrategy {

    private static final Logger log = Logger.getLogger(
        UnanimousVotingStrategy.class
    );

    @Override
    public boolean isComplete(
        Map<String, Set<String>> participantLikes,
        int totalParticipants
    ) {
        if (participantLikes.size() < totalParticipants) {
            return false; // не все участники проголосовали
        }

        Map<String, Long> movieCounts = countMovieLikes(participantLikes);

        // Проверяем, есть ли фильм, который лайкнули ВСЕ
        boolean hasUnanimousChoice = movieCounts
            .values()
            .stream()
            .anyMatch(count -> count == totalParticipants);

        if (hasUnanimousChoice) {
            log.infof(
                "Unanimous vote completed! Found movie(s) liked by all {} participants",
                totalParticipants
            );
        }

        return hasUnanimousChoice;
    }

    @Override
    public List<String> getMatchedMovies(
        Map<String, Set<String>> participantLikes,
        int totalParticipants
    ) {
        Map<String, Long> movieCounts = countMovieLikes(participantLikes);

        return movieCounts
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() == totalParticipants)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Подсчитывает количество лайков для каждого фильма
     *
     * @param participantLikes лайки участников
     * @return Map<movieId, количество лайков>
     */
    private Map<String, Long> countMovieLikes(
        Map<String, Set<String>> participantLikes
    ) {
        return participantLikes
            .values()
            .stream()
            .flatMap(Set::stream)
            .collect(
                Collectors.groupingBy(movieId -> movieId, Collectors.counting())
            );
    }
}
