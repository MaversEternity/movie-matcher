package com.moviematcher.infrastructure.adapter;

import com.moviematcher.entity.Movie;
import com.moviematcher.model.RoomFilters;

import java.util.List;
import java.util.Optional;

/**
 * Adapter Pattern - единый интерфейс для работы с разными источниками данных о фильмах
 *
 * Применение паттерна Адаптер позволяет:
 * - Унифицировать работу с разными источниками (БД, TMDB API, OMDB API)
 * - Изолировать изменения во внешних API от бизнес-логики
 * - Легко добавлять новые источники данных (Open/Closed Principle)
 * - Соблюдать Dependency Inversion Principle (зависимость от абстракции)
 *
 * Реализации:
 * - DatabaseMovieDataSource - поиск в локальной БД
 * - TmdbApiDataSource - получение данных из TMDB API
 * - OmdbApiDataSource - получение данных из OMDB API
 */
public interface MovieDataSource {

    /**
     * Поиск фильмов по фильтрам с пагинацией
     *
     * @param filters фильтры (жанр, год, рейтинг, тип)
     * @param page номер страницы (начиная с 1)
     * @param pageSize размер страницы
     * @return список фильмов
     */
    List<Movie> findByFilters(RoomFilters filters, int page, int pageSize);

    /**
     * Поиск фильма по внешнему ID (IMDB ID или TMDB ID)
     *
     * @param externalId внешний идентификатор
     * @return фильм, если найден
     */
    Optional<Movie> findByExternalId(String externalId);

    /**
     * Проверка доступности источника данных
     *
     * @return true если источник доступен
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * Название источника данных для логирования
     */
    String getSourceName();
}
