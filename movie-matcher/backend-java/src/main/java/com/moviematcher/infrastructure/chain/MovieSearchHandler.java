package com.moviematcher.infrastructure.chain;

import com.moviematcher.entity.Movie;

import java.util.Optional;

/**
 * Chain of Responsibility Pattern - базовый класс для цепочки поиска фильмов
 *
 * Применение паттерна Цепочка обязанностей позволяет:
 * - Создать последовательность обработчиков: БД → TMDB → OMDB
 * - Каждый обработчик либо обрабатывает запрос, либо передает следующему
 * - Автоматическое сохранение найденных фильмов в БД (обогащение)
 * - Легко добавлять/удалять обработчики (Open/Closed Principle)
 *
 * Цепочка:
 * 1. DatabaseSearchHandler - ищет в локальной БД
 * 2. TmdbSearchHandler - ищет в TMDB API, сохраняет в БД
 * 3. OmdbSearchHandler - ищет в OMDB API, сохраняет в БД
 */
public abstract class MovieSearchHandler {

    /**
     * Следующий обработчик в цепочке
     */
    protected MovieSearchHandler next;

    /**
     * Установить следующий обработчик в цепочке
     *
     * @param handler следующий обработчик
     * @return текущий обработчик (для fluent API)
     */
    public MovieSearchHandler setNext(MovieSearchHandler handler) {
        this.next = handler;
        return handler;
    }

    /**
     * Поиск фильма в текущем источнике
     * Если не найден - передает запрос следующему обработчику
     *
     * @param query поисковой запрос (название фильма)
     * @return фильм, если найден в любом источнике цепочки
     */
    public abstract Optional<Movie> search(String query);

    /**
     * Передать поиск следующему обработчику в цепочке
     *
     * @param query поисковой запрос
     * @return результат поиска следующего обработчика
     */
    protected Optional<Movie> searchNext(String query) {
        if (next != null) {
            return next.search(query);
        }
        return Optional.empty();
    }

    /**
     * Название обработчика для логирования
     */
    public abstract String getHandlerName();
}
