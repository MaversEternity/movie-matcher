package com.moviematcher.infrastructure.chain;

import com.moviematcher.client.tmdb.TmdbRestClient;
import com.moviematcher.entity.Movie;
import com.moviematcher.infrastructure.mapper.TmdbMovieMapper;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Обработчик поиска через TMDB API (второй в цепочке)
 *
 * При успешном поиске:
 * 1. Получает данные из TMDB на русском языке
 * 2. Конвертирует в Movie entity
 * 3. СОХРАНЯЕТ В БД (обогащение!)
 * 4. Возвращает фильм
 *
 * Если не найден - передает OMDB
 */
@ApplicationScoped
public class TmdbSearchHandler extends MovieSearchHandler {

    private static final Logger log = Logger.getLogger(TmdbSearchHandler.class);

    @ConfigProperty(name = "tmdb.api.key")
    String apiKey;

    @RestClient
    final TmdbRestClient tmdbClient;

    final TmdbMovieMapper movieMapper;

    @jakarta.inject.Inject
    public TmdbSearchHandler(
        TmdbRestClient tmdbClient,
        TmdbMovieMapper movieMapper
    ) {
        this.tmdbClient = tmdbClient;
        this.movieMapper = movieMapper;
    }

    @Override
    public Optional<Movie> search(String query) {
        log.debugf("Searching for '{}' in TMDB API", query);

        try {
            // Поиск по названию на русском
            var searchResponse = tmdbClient.searchMovies(
                apiKey,
                query,
                "ru-RU",
                1,
                false,
                null
            );

            if (
                searchResponse.results() != null &&
                !searchResponse.results().isEmpty()
            ) {
                var firstResult = searchResponse.results().get(0);

                // Получаем полные детали
                var movieDetails = tmdbClient.getMovieDetails(
                    firstResult.id(),
                    apiKey,
                    "ru-RU"
                );
                var credits = tmdbClient.getMovieCredits(
                    firstResult.id(),
                    apiKey,
                    "ru-RU"
                );

                Movie movie = movieMapper.toMovie(movieDetails, credits);

                log.infof(
                    "Found movie '{}' in TMDB (IMDB: {})",
                    movie.title,
                    movie.imdbId
                );

                // ВАЖНО: Сохраняем в БД для будущих запросов!
                movie.persist();
                log.infof("Saved movie '{}' to database from TMDB", movie.title);

                return Optional.of(movie);
            }

            log.debugf(
                "Movie '{}' not found in TMDB, passing to next handler",
                query
            );
        } catch (Exception e) {
            log.errorf("Error searching TMDB for '{}'", query, e);
        }

        // Если не нашли - передаем следующему обработчику (OMDB)
        return searchNext(query);
    }

    @Override
    public String getHandlerName() {
        return "TmdbSearchHandler";
    }
}
