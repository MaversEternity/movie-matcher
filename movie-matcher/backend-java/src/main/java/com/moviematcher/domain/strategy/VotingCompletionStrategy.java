package com.moviematcher.domain.strategy;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Strategy Pattern для определения условий завершения голосования
 *
 * Применение паттерна Стратегия позволяет:
 * - Инкапсулировать различные алгоритмы завершения голосования
 * - Легко добавлять новые стратегии без изменения существующего кода (Open/Closed Principle)
 * - Делать стратегии взаимозаменяемыми (Liskov Substitution Principle)
 */
public interface VotingCompletionStrategy {

    /**
     * Проверяет, завершено ли голосование согласно стратегии
     *
     * @param participantLikes Map<participantId, Set<movieId>> - лайки каждого участника
     * @param totalParticipants общее количество участников
     * @return true если условие завершения выполнено
     */
    boolean isComplete(Map<String, Set<String>> participantLikes, int totalParticipants);

    /**
     * Возвращает список фильмов, которые удовлетворяют условию стратегии
     *
     * @param participantLikes Map<participantId, Set<movieId>> - лайки каждого участника
     * @param totalParticipants общее количество участников
     * @return список ID фильмов, которые прошли по условию
     */
    List<String> getMatchedMovies(Map<String, Set<String>> participantLikes, int totalParticipants);
}
