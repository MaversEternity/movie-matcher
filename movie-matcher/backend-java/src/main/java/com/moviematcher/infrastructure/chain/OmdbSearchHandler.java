package com.moviematcher.infrastructure.chain;

import com.moviematcher.client.OmdbDetailResponse;
import com.moviematcher.client.OmdbRestClient;
import com.moviematcher.entity.Movie;
import com.moviematcher.infrastructure.mapper.OmdbMovieMapper;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Обработчик поиска через OMDB API (последний в цепочке)
 *
 * При успешном поиске:
 * 1. Получает данные из OMDB
 * 2. Конвертирует в Movie entity
 * 3. СОХРАНЯЕТ В БД (обогащение!)
 * 4. Возвращает фильм
 */
@ApplicationScoped
public class OmdbSearchHandler extends MovieSearchHandler {

    private static final Logger log = Logger.getLogger(OmdbSearchHandler.class);

    @ConfigProperty(name = "omdb.api.key")
    String apiKey;

    @RestClient
    final OmdbRestClient omdbClient;

    private final OmdbMovieMapper movieMapper;

    @jakarta.inject.Inject
    public OmdbSearchHandler(
        @RestClient OmdbRestClient omdbClient,
        OmdbMovieMapper movieMapper
    ) {
        this.omdbClient = omdbClient;
        this.movieMapper = movieMapper;
    }

    @Override
    public Optional<Movie> search(String query) {
        log.debugf("Searching for '{}' in OMDB API", query);

        try {
            OmdbDetailResponse response = omdbClient
                .searchByTitle(apiKey, query, "short")
                .await()
                .indefinitely();

            if ("True".equals(response.response())) {
                log.infof(
                    "Found movie '{}' in OMDB API (IMDB: {})",
                    response.title(),
                    response.imdbId()
                );

                // Конвертируем в Movie entity
                Movie movie = movieMapper.toMovie(response);

                // ВАЖНО: Сохраняем в БД для будущих запросов!
                movie.persist();
                log.infof(
                    "Saved movie '{}' to database from OMDB",
                    movie.title
                );

                return Optional.of(movie);
            }

            log.debugf("Movie '{}' not found in OMDB API", query);
        } catch (Exception e) {
            log.errorf("Error searching OMDB API for '{}'", query, e);
        }

        // Это последний обработчик, возвращаем пустой результат
        return Optional.empty();
    }

    @Override
    public String getHandlerName() {
        return "OmdbSearchHandler";
    }
}
