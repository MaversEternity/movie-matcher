package com.moviematcher.service;

import com.moviematcher.domain.model.Participant;
import com.moviematcher.domain.model.RoomAggregate;
import com.moviematcher.entity.Movie;
import com.moviematcher.infrastructure.adapter.DatabaseMovieDataSource;
import com.moviematcher.infrastructure.adapter.TmdbApiDataSource;
import com.moviematcher.model.MovieData;
import com.moviematcher.model.RoomFilters;
import com.moviematcher.model.ServerMessage;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * Application Service для подбора и подачи фильмов в голосовании
 *
 * Отвечает за:
 * - Подбор фильмов по фильтрам участников
 * - Стриминг фильмов в комнату
 * - Управление очередностью показа (круговая система)
 */
@ApplicationScoped
public class MovieSelectionService {

    private static final Logger log = Logger.getLogger(
        MovieSelectionService.class
    );

    private final DatabaseMovieDataSource databaseSource;
    private final TmdbApiDataSource tmdbSource;
    private final WebSocketBroadcastService broadcastService;

    private static final int MOVIES_PER_BATCH = 20;

    @jakarta.inject.Inject
    public MovieSelectionService(
        DatabaseMovieDataSource databaseSource,
        TmdbApiDataSource tmdbSource,
        WebSocketBroadcastService broadcastService
    ) {
        this.databaseSource = databaseSource;
        this.tmdbSource = tmdbSource;
        this.broadcastService = broadcastService;
    }

    /**
     * Начать стриминг фильмов в комнату
     *
     * Логика:
     * 1. Загружаем фильмы для каждого участника по их фильтрам
     * 2. Добавляем в очереди VotingSession
     * 3. Начинаем поочередную подачу фильмов
     */
    public void startMovieStream(String roomId, RoomAggregate room) {
        log.infof("Starting movie stream for room {}", roomId);

        // Загружаем фильмы для каждого участника
        for (Participant participant : room.getParticipants()) {
            if (participant.getFilters() != null) {
                loadMoviesForParticipant(room, participant);
            }
        }

        // Начинаем асинхронную подачу фильмов
        streamMoviesAsync(roomId, room)
            .subscribe()
            .with(
                result ->
                    log.infof("Movie stream completed for room {}", roomId),
                error ->
                    log.errorf(
                        "Error in movie stream for room {}",
                        roomId,
                        error
                    )
            );
    }

    /**
     * Загрузить фильмы для участника по его фильтрам
     */
    private void loadMoviesForParticipant(
        RoomAggregate room,
        Participant participant
    ) {
        var filters = participant.getFilters();

        // Конвертируем domain.MovieFilters → model.RoomFilters для совместимости
        RoomFilters roomFilters = new RoomFilters(
            filters.getGenre(),
            filters.getYearFrom(),
            filters.getYearTo(),
            filters.getMinRating(),
            filters.getType()
        );

        // Сначала пробуем БД (быстрее)
        List<Movie> movies = databaseSource.findByFilters(
            roomFilters,
            1, // page
            MOVIES_PER_BATCH
        );

        // Если в БД мало - добавляем из TMDB
        if (movies.size() < MOVIES_PER_BATCH) {
            List<Movie> tmdbMovies = tmdbSource.findByFilters(
                roomFilters,
                1,
                MOVIES_PER_BATCH - movies.size()
            );
            movies.addAll(tmdbMovies);
        }

        // Конвертируем в IDs и добавляем в очередь
        List<String> movieIds = movies
            .stream()
            .map(m -> m.imdbId)
            .collect(Collectors.toList());

        room.addMoviesToParticipantQueue(participant.getId(), movieIds);

        log.infof(
            "Loaded {} movies for participant {} in room",
            movieIds.size(),
            participant.getId()
        );
    }

    /**
     * Асинхронная подача фильмов
     */
    private Uni<Void> streamMoviesAsync(String roomId, RoomAggregate room) {
        return Uni.createFrom()
            .voidItem()
            .onItem()
            .transformToUni(v -> sendNextMovie(roomId, room));
    }

    /**
     * Отправить следующий фильм
     */
    private Uni<Void> sendNextMovie(String roomId, RoomAggregate room) {
        if (room.getState() != com.moviematcher.domain.model.RoomState.VOTING) {
            log.infof("Voting ended in room {}, stopping stream", roomId);
            return Uni.createFrom().voidItem();
        }

        Optional<String> nextMovieId = room.getNextMovie();

        if (nextMovieId.isEmpty()) {
            log.infof("No more movies available for room {}", roomId);
            broadcastService.broadcast(
                roomId,
                new ServerMessage.NoMoreMovies()
            );
            return Uni.createFrom().voidItem();
        }

        // Получаем полную информацию о фильме
        Movie movie = Movie.findByImdbId(nextMovieId.get());

        if (movie == null) {
            log.warnf("Movie {} not found in DB, skipping", nextMovieId.get());
            // Пропускаем и берем следующий
            return Uni.createFrom()
                .voidItem()
                .onItem()
                .delayIt()
                .by(Duration.ofMillis(100))
                .onItem()
                .transformToUni(v -> sendNextMovie(roomId, room));
        }

        // Конвертируем в MovieData для отправки
        MovieData movieData = convertToMovieData(movie);

        // Отправляем фильм всем участникам
        broadcastService.broadcast(
            roomId,
            new ServerMessage.NewMovie(movieData)
        );

        log.debugf("Sent movie '{}' to room {}", movie.title, roomId);

        // Ждем немного перед следующим фильмом (чтобы не флудить)
        return Uni.createFrom()
            .voidItem()
            .onItem()
            .delayIt()
            .by(Duration.ofMillis(500))
            .onItem()
            .transformToUni(v -> sendNextMovie(roomId, room));
    }

    /**
     * Конвертация Movie entity → MovieData DTO
     */
    private MovieData convertToMovieData(Movie movie) {
        return new MovieData(
            movie.title,
            movie.year != null ? movie.year.toString() : "",
            "", // rated
            movie.runtime != null ? movie.runtime + " min" : "",
            movie.posterUrl,
            extractDirector(movie),
            extractActors(movie),
            movie.plot,
            extractCountry(movie),
            extractGenres(movie),
            movie.imdbRating != null ? movie.imdbRating.toString() : "",
            movie.imdbId
        );
    }

    private String extractDirector(Movie movie) {
        if (movie.credits == null) return "";

        return movie.credits
            .stream()
            .filter(c -> "director".equals(c.roleType))
            .map(c -> c.person.name)
            .findFirst()
            .orElse("");
    }

    private String extractActors(Movie movie) {
        if (movie.credits == null) return "";

        return movie.credits
            .stream()
            .filter(c -> "actor".equals(c.roleType))
            .limit(3)
            .map(c -> c.person.name)
            .collect(Collectors.joining(", "));
    }

    private String extractCountry(Movie movie) {
        if (movie.countries == null || movie.countries.isEmpty()) return "";

        return movie.countries
            .stream()
            .map(c -> c.name)
            .findFirst()
            .orElse("");
    }

    private String extractGenres(Movie movie) {
        if (movie.genres == null || movie.genres.isEmpty()) return "";

        return movie.genres
            .stream()
            .map(g -> g.name)
            .collect(Collectors.joining(", "));
    }
}
