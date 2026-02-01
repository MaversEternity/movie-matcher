package com.moviematcher.service;

import com.moviematcher.entity.Movie;
import com.moviematcher.infrastructure.chain.DatabaseSearchHandler;
import com.moviematcher.infrastructure.chain.MovieSearchHandler;
import com.moviematcher.infrastructure.chain.OmdbSearchHandler;
import com.moviematcher.infrastructure.chain.TmdbSearchHandler;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import org.jboss.logging.Logger;

/**
 * Application Service для поиска фильмов
 *
 * Использует Chain of Responsibility Pattern:
 * Database → TMDB (русский язык!) → OMDB (fallback)
 *
 * При нахождении фильма через API автоматически сохраняет его в БД
 */
@ApplicationScoped
public class MovieSearchService {

    private static final Logger log = Logger.getLogger(
        MovieSearchService.class
    );

    private final DatabaseSearchHandler databaseHandler;
    private final TmdbSearchHandler tmdbHandler;
    private final OmdbSearchHandler omdbHandler;

    private MovieSearchHandler searchChain;

    @jakarta.inject.Inject
    public MovieSearchService(
        DatabaseSearchHandler databaseHandler,
        TmdbSearchHandler tmdbHandler,
        OmdbSearchHandler omdbHandler
    ) {
        this.databaseHandler = databaseHandler;
        this.tmdbHandler = tmdbHandler;
        this.omdbHandler = omdbHandler;
    }

    /**
     * Инициализация цепочки обработчиков
     * DB → TMDB → OMDB
     *
     * TMDB приоритетнее OMDB потому что:
     * - Возвращает данные на русском языке
     * - Более полная информация
     * - Лучший API
     */
    @PostConstruct
    public void init() {
        log.infof("Initializing movie search chain: Database → TMDB → OMDB");

        // Строим цепочку: DB → TMDB → OMDB
        databaseHandler.setNext(tmdbHandler);
        tmdbHandler.setNext(omdbHandler);

        searchChain = databaseHandler;
    }

    /**
     * Поиск фильма по названию через цепочку обработчиков
     *
     * Поиск идет последовательно:
     * 1. Сначала в БД
     * 2. Если не найден - в TMDB API (с русским языком, и сохраняет в БД)
     * 3. Если не найден - в OMDB API (fallback, и сохраняет в БД)
     *
     * @param query название фильма
     * @return найденный фильм или empty
     */
    public Optional<Movie> searchMovie(String query) {
        log.infof("Searching for movie: '{}'", query);

        if (query == null || query.isBlank()) {
            log.warnf("Empty search query");
            return Optional.empty();
        }

        Optional<Movie> result = searchChain.search(query.trim());

        if (result.isPresent()) {
            log.infof(
                "Movie found: '{}' (IMDB: {})",
                result.get().title,
                result.get().imdbId
            );
        } else {
            log.infof("Movie not found: '{}'", query);
        }

        return result;
    }
}
