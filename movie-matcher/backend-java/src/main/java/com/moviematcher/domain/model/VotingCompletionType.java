package com.moviematcher.domain.model;

/**
 * Тип условия завершения голосования
 */
public enum VotingCompletionType {
    /**
     * Все участники (100%) должны лайкнуть один и тот же фильм
     */
    UNANIMOUS,

    /**
     * Большинство участников (70%) должны лайкнуть один и тот же фильм
     */
    MAJORITY
}
