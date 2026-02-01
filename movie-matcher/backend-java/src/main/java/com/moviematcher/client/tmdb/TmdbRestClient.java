package com.moviematcher.client.tmdb;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * REST клиент для TMDB API v3
 *
 * TMDB API Documentation: https://developers.themoviedb.org/3
 *
 * Ключевое преимущество TMDB - поддержка переводов!
 * Параметр language=ru-RU возвращает названия и описания на русском
 */
@Path("/3")
@RegisterRestClient(configKey = "tmdb-api")
@Produces(MediaType.APPLICATION_JSON)
public interface TmdbRestClient {

    /**
     * Поиск фильмов по названию
     *
     * @param apiKey API ключ
     * @param query поисковой запрос
     * @param language язык (ru-RU для русского)
     * @param page номер страницы
     * @param includeAdult включать взрослый контент
     * @param year фильтр по году
     * @return результаты поиска
     */
    @GET
    @Path("/search/movie")
    TmdbSearchResponse searchMovies(
        @QueryParam("api_key") String apiKey,
        @QueryParam("query") String query,
        @QueryParam("language") @DefaultValue("ru-RU") String language,
        @QueryParam("page") @DefaultValue("1") Integer page,
        @QueryParam("include_adult") @DefaultValue("false") Boolean includeAdult,
        @QueryParam("year") Integer year
    );

    /**
     * Получить детали фильма по ID
     *
     * @param movieId TMDB ID фильма
     * @param apiKey API ключ
     * @param language язык (ru-RU для русского)
     * @return детали фильма
     */
    @GET
    @Path("/movie/{movie_id}")
    TmdbMovieResponse getMovieDetails(
        @PathParam("movie_id") Long movieId,
        @QueryParam("api_key") String apiKey,
        @QueryParam("language") @DefaultValue("ru-RU") String language
    );

    /**
     * Получить актеров и съемочную группу
     *
     * @param movieId TMDB ID фильма
     * @param apiKey API ключ
     * @param language язык
     * @return credits (cast & crew)
     */
    @GET
    @Path("/movie/{movie_id}/credits")
    TmdbCreditsResponse getMovieCredits(
        @PathParam("movie_id") Long movieId,
        @QueryParam("api_key") String apiKey,
        @QueryParam("language") @DefaultValue("ru-RU") String language
    );

    /**
     * Discover movies по фильтрам
     * Более мощный метод чем search - позволяет фильтровать по жанрам, рейтингу и т.д.
     * ВАЖНО: Используется для РАНДОМНОГО подбора фильмов с фильтрами!
     *
     * @param apiKey API ключ
     * @param language язык
     * @param sortBy сортировка (popularity.desc, vote_average.desc, release_date.desc)
     * @param page номер страницы
     * @param withGenres жанры (через запятую, например "28,12")
     * @param primaryReleaseYearGte год от
     * @param primaryReleaseYearLte год до
     * @param voteAverageGte минимальный рейтинг
     * @return результаты поиска
     */
    @GET
    @Path("/discover/movie")
    TmdbSearchResponse discoverMovies(
        @QueryParam("api_key") String apiKey,
        @QueryParam("language") @DefaultValue("ru-RU") String language,
        @QueryParam("sort_by") @DefaultValue("popularity.desc") String sortBy,
        @QueryParam("page") @DefaultValue("1") Integer page,
        @QueryParam("with_genres") String withGenres,
        @QueryParam("primary_release_date.gte") Integer primaryReleaseYearGte,
        @QueryParam("primary_release_date.lte") Integer primaryReleaseYearLte,
        @QueryParam("vote_average.gte") Double voteAverageGte,
        @QueryParam("vote_count.gte") @DefaultValue("100") Integer voteCountGte
    );

    /**
     * Получить фильм по внешнему ID (IMDB)
     *
     * @param externalId IMDB ID (например "tt0111161")
     * @param apiKey API ключ
     * @param language язык
     * @param externalSource источник ID (imdb_id)
     * @return поисковая выдача
     */
    @GET
    @Path("/find/{external_id}")
    TmdbFindResponse findByExternalId(
        @PathParam("external_id") String externalId,
        @QueryParam("api_key") String apiKey,
        @QueryParam("language") @DefaultValue("ru-RU") String language,
        @QueryParam("external_source") @DefaultValue("imdb_id") String externalSource
    );
}
