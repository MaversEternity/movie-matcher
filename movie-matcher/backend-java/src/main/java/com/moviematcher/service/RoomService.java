package com.moviematcher.service;

import com.moviematcher.client.OmdbClient;
import com.moviematcher.model.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class RoomService {

    private static final int MATCH_THRESHOLD = 3;

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    private final OmdbClient omdbClient;
    private final WebSocketBroadcastService broadcastService;

    public Room createRoom(RoomFilters filters, String hostId) {
        Room room = new Room(UUID.randomUUID().toString(), filters, hostId);
        rooms.put(room.getId(), room);
        log.info("Created room: {}", room.getId());
        return room;
    }

    public Optional<Room> getRoom(String roomId) {
        return Optional.ofNullable(rooms.get(roomId));
    }

    public boolean joinRoom(String roomId, String participantId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return false;
        }

        boolean added = room.addParticipant(participantId);
        if (added) {
            log.info("Participant {} joined room {}", participantId, roomId);
            broadcastService.broadcast(
                roomId,
                new ServerMessage.ParticipantJoined(participantId)
            );
        }
        return added;
    }

    public void leaveRoom(String roomId, String participantId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return;
        }

        room.removeParticipant(participantId);
        broadcastService.broadcast(
            roomId,
            new ServerMessage.ParticipantLeft(participantId)
        );
        log.info("Participant {} left room {}", participantId, roomId);

        // If only host remains, end matching
        if (
            room.getParticipants().size() == 1 &&
            room.getParticipants().get(0).equals(room.getHostId())
        ) {
            log.info("Only host remains in room {}, ending matching", roomId);
            endMatching(roomId);
        }
    }

    public void updateFilters(String roomId, RoomFilters filters) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.setFilters(filters);
            log.info("Updated filters for room {}: {}", roomId, filters);
        }
    }

    public void startMatching(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return;
        }

        room.start();
        broadcastService.broadcast(roomId, new ServerMessage.MatchingStarted());
        log.info("Room {} started matching", roomId);

        // Start movie streaming in background
        streamMoviesInfinitely(roomId)
            .subscribe()
            .with(
                result ->
                    log.info("Movie streaming completed for room {}", roomId),
                failure ->
                    log.error(
                        "Error streaming movies for room {}",
                        roomId,
                        failure
                    )
            );
    }

    private Uni<Void> streamMoviesInfinitely(String roomId) {
        return Uni.createFrom()
            .voidItem()
            .onItem()
            .transformToUni(v -> streamNextPage(roomId));
    }

    private Uni<Void> streamNextPage(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null || !room.isActive()) {
            log.info(
                "Room {} is no longer active, stopping movie stream",
                roomId
            );
            return Uni.createFrom().voidItem();
        }

        int currentPage = room.getCurrentPage();

        return omdbClient
            .fetchMoviesByPage(room.getFilters(), currentPage)
            .onItem()
            .transformToUni(page -> {
                // Filter out duplicates
                List<MovieData> newMovies = page
                    .movies()
                    .stream()
                    .filter(movie -> !room.hasMovieBeenSent(movie.imdbId()))
                    .peek(movie -> room.markMovieAsSent(movie.imdbId()))
                    .toList();

                room.incrementPage();

                if (newMovies.isEmpty() && !page.hasMore()) {
                    log.info(
                        "Exhausted all pages for room {}, stopping stream",
                        roomId
                    );
                    broadcastService.broadcast(
                        roomId,
                        new ServerMessage.StreamingEnded()
                    );
                    return Uni.createFrom().voidItem();
                }

                if (newMovies.isEmpty()) {
                    log.info(
                        "Page {} had no new movies, trying next page",
                        currentPage
                    );
                    return Uni.createFrom()
                        .voidItem()
                        .onItem()
                        .delayIt()
                        .by(Duration.ofMillis(500))
                        .onItem()
                        .transformToUni(v -> streamNextPage(roomId));
                }

                log.info(
                    "Fetched {} new movies from page {} for room {}, has_more: {}",
                    newMovies.size(),
                    currentPage,
                    roomId,
                    page.hasMore()
                );

                // Broadcast movies with delay between each
                return broadcastMoviesSequentially(roomId, newMovies, 0)
                    .onItem()
                    .transformToUni(v -> {
                        if (!page.hasMore()) {
                            log.info(
                                "No more pages available for room {}, stopping stream",
                                roomId
                            );
                            broadcastService.broadcast(
                                roomId,
                                new ServerMessage.StreamingEnded()
                            );
                            return Uni.createFrom().voidItem();
                        }

                        // Continue to next page after delay
                        return Uni.createFrom()
                            .voidItem()
                            .onItem()
                            .delayIt()
                            .by(Duration.ofSeconds(1))
                            .onItem()
                            .transformToUni(v2 -> streamNextPage(roomId));
                    });
            })
            .onFailure()
            .recoverWithUni(error -> {
                log.error(
                    "Error fetching movies for room {} on page {}",
                    roomId,
                    currentPage,
                    error
                );
                endMatching(roomId);
                return Uni.createFrom().voidItem();
            });
    }

    private Uni<Void> broadcastMoviesSequentially(
        String roomId,
        List<MovieData> movies,
        int index
    ) {
        if (index >= movies.size()) {
            return Uni.createFrom().voidItem();
        }

        Room room = rooms.get(roomId);
        if (room == null || !room.isActive()) {
            return Uni.createFrom().voidItem();
        }

        MovieData movie = movies.get(index);
        broadcastService.broadcast(roomId, new ServerMessage.NewMovie(movie));

        return Uni.createFrom()
            .voidItem()
            .onItem()
            .delayIt()
            .by(Duration.ofMillis(200))
            .onItem()
            .transformToUni(v ->
                broadcastMoviesSequentially(roomId, movies, index + 1)
            );
    }

    public void handleMovieLiked(
        String roomId,
        String participantId,
        String imdbId
    ) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return;
        }

        room.addLike(participantId, imdbId);
        log.info(
            "Participant {} liked movie {} in room {}",
            participantId,
            imdbId,
            roomId
        );

        // Get updated likes (we don't cache all movies, so pass empty list)
        List<MovieData> allMovies = List.of();
        List<ParticipantLikes> allLikes = room.getAllParticipantLikes(
            allMovies
        );
        List<MovieData> commonLikes = room.getCommonLikes(allMovies);

        broadcastService.broadcast(
            roomId,
            new ServerMessage.LikesUpdated(allLikes, commonLikes)
        );

        // Check for match
        if (commonLikes.size() >= MATCH_THRESHOLD) {
            log.info(
                "Match found! {} common movies in room {}",
                commonLikes.size(),
                roomId
            );
            broadcastService.broadcast(
                roomId,
                new ServerMessage.MatchFound(allLikes, commonLikes)
            );
        }
    }

    public void endMatching(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return;
        }

        room.setActive(false);

        List<MovieData> allMovies = List.of();
        List<ParticipantLikes> allLikes = room.getAllParticipantLikes(
            allMovies
        );
        List<MovieData> commonLikes = room.getCommonLikes(allMovies);

        broadcastService.broadcast(
            roomId,
            new ServerMessage.MatchingEnded(allLikes, commonLikes)
        );
        log.info("Room {} ended matching", roomId);
    }

    public RoomInfo getRoomInfo(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return null;
        }

        return new RoomInfo(
            room.getId(),
            room.getFilters(),
            room.getParticipants().size(),
            room.isActive()
        );
    }

    public RoomState getRoomState(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return null;
        }

        List<MovieData> allMovies = List.of();
        return new RoomState(
            room.getId(),
            room.getFilters(),
            room.getParticipants(),
            room.isActive(),
            room.getHostId(),
            room.getAllParticipantLikes(allMovies),
            room.getCommonLikes(allMovies)
        );
    }
}
