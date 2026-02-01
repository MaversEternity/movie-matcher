package com.moviematcher.infrastructure.adapter;

import com.moviematcher.entity.Movie;
import com.moviematcher.model.RoomFilters;
import com.moviematcher.repository.MovieRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import org.jboss.logging.Logger;

/**
 * Адаптер для работы с локальной БД фильмов
 *
 * Single Responsibility: отвечает только за получение данных из БД
 * Dependency Inversion: зависит от MovieRepository (абстракция)
 */
@ApplicationScoped
public class DatabaseMovieDataSource implements MovieDataSource {

    private static final Logger log = Logger.getLogger(
        DatabaseMovieDataSource.class
    );

    private final MovieRepository movieRepository;

    @jakarta.inject.Inject
    public DatabaseMovieDataSource(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    @Override
    public List<Movie> findByFilters(
        RoomFilters filters,
        int page,
        int pageSize
    ) {
        log.debugf(
            "Searching movies in DB with filters: {}, page: {}, size: {}",
            filters,
            page,
            pageSize
        );

        return movieRepository.findByFilters(
            filters.getGenre(),
            filters.getYearFrom(),
            filters.getYearTo(),
            filters.getMinRating(),
            filters.getType(),
            page,
            pageSize
        );
    }

    @Override
    public Optional<Movie> findByExternalId(String externalId) {
        log.debugf("Searching movie in DB by external ID: {}", externalId);

        // Сначала ищем по IMDB ID
        Movie movie = Movie.findByImdbId(externalId);
        if (movie != null) {
            return Optional.of(movie);
        }

        // Можно добавить поиск по TMDB ID
        // Movie byTmdbId = movieRepository.findByTmdbId(externalId);

        return Optional.empty();
    }

    @Override
    public String getSourceName() {
        return "Database";
    }
}
