package com.moviematcher.domain.strategy;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.*;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * Стратегия голосования большинством
 *
 * Большинство участников (70%) должны лайкнуть один и тот же фильм для завершения голосования
 *
 * Single Responsibility: отвечает только за логику голосования большинством
 */
@ApplicationScoped
public class MajorityVotingStrategy implements VotingCompletionStrategy {

    private static final Logger log = Logger.getLogger(
        MajorityVotingStrategy.class
    );

    private static final double MAJORITY_THRESHOLD = 0.70; // 70%

    @Override
    public boolean isComplete(
        Map<String, Set<String>> participantLikes,
        int totalParticipants
    ) {
        if (participantLikes.isEmpty()) {
            return false;
        }

        int requiredVotes = calculateRequiredVotes(totalParticipants);
        Map<String, Long> movieCounts = countMovieLikes(participantLikes);

        // Проверяем, есть ли фильм, который лайкнули >= 70%
        boolean hasMajorityChoice = movieCounts
            .values()
            .stream()
            .anyMatch(count -> count >= requiredVotes);

        if (hasMajorityChoice) {
            log.infof(
                "Majority vote completed! Found movie(s) liked by at least {}/{} participants (70%)",
                requiredVotes,
                totalParticipants
            );
        }

        return hasMajorityChoice;
    }

    @Override
    public List<String> getMatchedMovies(
        Map<String, Set<String>> participantLikes,
        int totalParticipants
    ) {
        int requiredVotes = calculateRequiredVotes(totalParticipants);
        Map<String, Long> movieCounts = countMovieLikes(participantLikes);

        return movieCounts
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue() >= requiredVotes)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Вычисляет необходимое количество голосов для большинства (70%)
     * Округляется вверх (ceil)
     */
    private int calculateRequiredVotes(int totalParticipants) {
        return (int) Math.ceil(totalParticipants * MAJORITY_THRESHOLD);
    }

    /**
     * Подсчитывает количество лайков для каждого фильма
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
