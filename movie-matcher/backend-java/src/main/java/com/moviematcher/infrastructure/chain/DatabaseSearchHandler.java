package com.moviematcher.infrastructure.chain;

import com.moviematcher.entity.Movie;
import com.moviematcher.repository.MovieRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import org.jboss.logging.Logger;

/**
 * Первый обработчик в цепочке - поиск в локальной БД
 *
 * Если фильм найден в БД - возвращаем его
 * Если не найден - передаем запрос следующему обработчику (TMDB или OMDB)
 */
@ApplicationScoped
public class DatabaseSearchHandler extends MovieSearchHandler {

    private static final Logger log = Logger.getLogger(
        DatabaseSearchHandler.class
    );

    private final MovieRepository movieRepository;

    @jakarta.inject.Inject
    public DatabaseSearchHandler(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Override
    public Optional<Movie> search(String query) {
        log.debugf("Searching for '{}' in Database", query);

        List<Movie> results = movieRepository.quickSearchByTitle(query, 1);

        if (!results.isEmpty()) {
            Movie movie = results.get(0);
            log.infof(
                "Found movie '{}' in Database (IMDB: {})",
                movie.title,
                movie.imdbId
            );
            return Optional.of(movie);
        }

        log.debugf(
            "Movie '{}' not found in Database, passing to next handler",
            query
        );
        return searchNext(query);
    }

    @Override
    public String getHandlerName() {
        return "DatabaseSearchHandler";
    }
}
