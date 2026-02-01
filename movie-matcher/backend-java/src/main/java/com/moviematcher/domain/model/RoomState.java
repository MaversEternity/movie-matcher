package com.moviematcher.domain.model;

/**
 * Состояние комнаты
 */
public enum RoomState {
    /**
     * Ожидание участников, настройка фильтров
     */
    WAITING,

    /**
     * Голосование в процессе
     */
    VOTING,

    /**
     * Голосование завершено
     */
    COMPLETED
}
