package com.moviematcher.domain.model;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Участник комнаты - Entity по DDD
 *
 * Инкапсулирует данные и поведение участника:
 * - Фильтры для подбора фильмов
 * - Фильмы, добавленные вручную через поиск
 * - Лайки и дизлайки
 * - Статус готовности к голосованию
 */
public class Participant {

    private final String id;
    private final boolean isHost;

    // Фильтры участника для автоматического подбора
    private MovieFilters filters;

    // Фильмы, добавленные участником вручную через поиск
    // ВАЖНО: Эти фильмы показываются первыми в голосовании!
    private final Queue<String> manuallySelectedMovieIds = new LinkedList<>();

    // Лайкнутые фильмы
    private final Set<String> likedMovies = new HashSet<>();

    // Дизлайкнутые фильмы
    private final Set<String> dislikedMovies = new HashSet<>();

    // Готовность к голосованию
    private boolean readyToVote = false;

    public Participant(String id, boolean isHost) {
        this.id = id;
        this.isHost = isHost;
    }

    /**
     * Установить фильтры для подбора
     */
    public void setFilters(MovieFilters filters) {
        if (filters == null) {
            throw new IllegalArgumentException("Filters cannot be null");
        }
        this.filters = filters;
    }

    /**
     * Добавить фильм вручную (через поиск)
     * Эти фильмы будут показаны первыми!
     */
    public void addManuallySelectedMovie(String movieId) {
        if (movieId == null || movieId.isBlank()) {
            throw new IllegalArgumentException(
                "Movie ID cannot be null or empty"
            );
        }
        manuallySelectedMovieIds.offer(movieId);
    }

    /**
     * Получить следующий вручную выбранный фильм
     */
    public Optional<String> pollManualMovie() {
        return Optional.ofNullable(manuallySelectedMovieIds.poll());
    }

    /**
     * Есть ли еще вручную выбранные фильмы?
     */
    public boolean hasManualMovies() {
        return !manuallySelectedMovieIds.isEmpty();
    }

    /**
     * Лайкнуть фильм
     */
    public void likeMovie(String movieId) {
        likedMovies.add(movieId);
        dislikedMovies.remove(movieId); // Убираем из дизлайков если был
    }

    /**
     * Дизлайкнуть фильм
     */
    public void dislikeMovie(String movieId) {
        dislikedMovies.add(movieId);
        likedMovies.remove(movieId); // Убираем из лайков если был
    }

    /**
     * Пометить как готового к голосованию
     */
    public void markReady() {
        if (filters == null && manuallySelectedMovieIds.isEmpty()) {
            throw new IllegalStateException(
                "Participant must set filters or add movies before marking ready"
            );
        }
        this.readyToVote = true;
    }

    /**
     * Получить копию лайкнутых фильмов (defensive copy)
     */
    public Set<String> getLikedMovies() {
        return new HashSet<>(likedMovies);
    }

    /**
     * Проверить, лайкнул ли участник этот фильм
     */
    public boolean hasLiked(String movieId) {
        return likedMovies.contains(movieId);
    }

    /**
     * Получить ID участника
     */
    public String getId() {
        return id;
    }

    /**
     * Проверить, готов ли участник к голосованию
     */
    public boolean isReadyToVote() {
        return readyToVote;
    }

    /**
     * Получить фильтры участника
     */
    public MovieFilters getFilters() {
        return filters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Participant that = (Participant) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
