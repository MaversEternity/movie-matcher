package com.moviematcher.infrastructure.adapter;

import com.moviematcher.client.tmdb.TmdbRestClient;
import com.moviematcher.client.tmdb.TmdbSearchResponse;
import com.moviematcher.entity.Movie;
import com.moviematcher.infrastructure.mapper.TmdbMovieMapper;
import com.moviematcher.model.RoomFilters;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Адаптер для работы с TMDB API
 *
 * Adapter Pattern - унифицирует работу с TMDB API
 *
 * ПРЕИМУЩЕСТВА TMDB:
 * - Поддержка русского языка (language=ru-RU)
 * - Discover API для фильтрации
 * - Более полные данные о фильмах
 * - Бесплатный API с хорошими лимитами
 */
@ApplicationScoped
public class TmdbApiDataSource implements MovieDataSource {

    private static final Logger log = Logger.getLogger(TmdbApiDataSource.class);

    @ConfigProperty(name = "tmdb.api.key")
    String apiKey;

    @RestClient
    final TmdbRestClient tmdbClient;

    final TmdbMovieMapper movieMapper;

    @jakarta.inject.Inject
    public TmdbApiDataSource(
        @RestClient TmdbRestClient tmdbClient,
        TmdbMovieMapper movieMapper
    ) {
        this.tmdbClient = tmdbClient;
        this.movieMapper = movieMapper;
    }

    @Override
    public List<Movie> findByFilters(
        RoomFilters filters,
        int page,
        int pageSize
    ) {
        log.debugf(
            "Searching movies in TMDB with filters: {}, page: {}",
            filters,
            page
        );

        try {
            // TMDB Discover API - мощный метод для фильтрации
            TmdbSearchResponse response = tmdbClient.discoverMovies(
                apiKey,
                "ru-RU", // ВАЖНО: Русский язык!
                "popularity.desc", // Сортировка по популярности
                page,
                mapGenreToTmdbId(filters.getGenre()),
                filters.getYearFrom(),
                filters.getYearTo(),
                filters.getMinRating() != null
                    ? filters.getMinRating().doubleValue()
                    : null,
                100 // Минимум 100 голосов для фильтрации мусора
            );

            if (response == null || response.results() == null) {
                return List.of();
            }

            // Конвертируем результаты в Movie entities
            List<Movie> movies = response
                .results()
                .stream()
                .limit(pageSize)
                .map(movieMapper::toMovieFromSearchResult)
                .collect(Collectors.toList());

            log.infof("Found {} movies from TMDB", movies.size());
            return movies;
        } catch (Exception e) {
            log.errorf("Error searching TMDB", e);
            return List.of();
        }
    }

    @Override
    public Optional<Movie> findByExternalId(String externalId) {
        log.debugf("Searching movie in TMDB by external ID: {}", externalId);

        try {
            var findResponse = tmdbClient.findByExternalId(
                externalId,
                apiKey,
                "ru-RU",
                "imdb_id"
            );

            if (
                findResponse.movieResults() != null &&
                !findResponse.movieResults().isEmpty()
            ) {
                var searchResult = findResponse.movieResults().get(0);

                // Получаем полные детали фильма
                var movieDetails = tmdbClient.getMovieDetails(
                    searchResult.id(),
                    apiKey,
                    "ru-RU"
                );
                var credits = tmdbClient.getMovieCredits(
                    searchResult.id(),
                    apiKey,
                    "ru-RU"
                );

                Movie movie = movieMapper.toMovie(movieDetails, credits);
                log.infof("Found movie in TMDB: {}", movie.title);
                return Optional.of(movie);
            }

            return Optional.empty();
        } catch (Exception e) {
            log.errorf(
                "Error searching TMDB by external ID: {}",
                externalId,
                e
            );
            return Optional.empty();
        }
    }

    @Override
    public String getSourceName() {
        return "TMDB";
    }

    /**
     * Маппинг названия жанра в TMDB ID
     *
     * TMDB Genre IDs:
     * 28 - Боевик (Action)
     * 12 - Приключения (Adventure)
     * 16 - Мультфильм (Animation)
     * 35 - Комедия (Comedy)
     * 80 - Криминал (Crime)
     * 99 - Документальный (Documentary)
     * 18 - Драма (Drama)
     * 10751 - Семейный (Family)
     * 14 - Фэнтези (Fantasy)
     * 36 - История (History)
     * 27 - Ужасы (Horror)
     * 10402 - Музыка (Music)
     * 9648 - Детектив (Mystery)
     * 10749 - Мелодрама (Romance)
     * 878 - Фантастика (Science Fiction)
     * 10770 - Телевизионный фильм (TV Movie)
     * 53 - Триллер (Thriller)
     * 10752 - Военный (War)
     * 37 - Вестерн (Western)
     */
    private String mapGenreToTmdbId(String genreName) {
        if (genreName == null || genreName.isBlank()) {
            return null;
        }

        return switch (genreName.toLowerCase()) {
            case "боевик", "action" -> "28";
            case "приключения", "adventure" -> "12";
            case "мультфильм", "animation" -> "16";
            case "комедия", "comedy" -> "35";
            case "криминал", "crime" -> "80";
            case "документальный", "documentary" -> "99";
            case "драма", "drama" -> "18";
            case "семейный", "family" -> "10751";
            case "фэнтези", "fantasy" -> "14";
            case "история", "history" -> "36";
            case "ужасы", "horror" -> "27";
            case "музыка", "music" -> "10402";
            case "детектив", "mystery" -> "9648";
            case "мелодрама", "romance" -> "10749";
            case "фантастика", "science fiction", "sci-fi" -> "878";
            case "триллер", "thriller" -> "53";
            case "военный", "war" -> "10752";
            case "вестерн", "western" -> "37";
            default -> null;
        };
    }
}
