package com.moviematcher.service;

import com.moviematcher.client.OmdbClient;
import com.moviematcher.model.*;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;

@ApplicationScoped
public class RoomService {

    private static final Logger LOG = Logger.getLogger(RoomService.class);
    private static final int MATCH_THRESHOLD = 3;

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();

    @Inject
    OmdbClient omdbClient;

    @Inject
    WebSocketBroadcastService broadcastService;

    public Room createRoom(RoomFilters filters, String hostId) {
        Room room = new Room(UUID.randomUUID().toString(), filters, hostId);
        rooms.put(room.getId(), room);
        LOG.infof("Created room: %s", room.getId());
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
            LOG.infof("Participant %s joined room %s", participantId, roomId);
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
        LOG.infof("Participant %s left room %s", participantId, roomId);

        // If only host remains, end matching
        if (
            room.getParticipants().size() == 1 &&
            room.getParticipants().get(0).equals(room.getHostId())
        ) {
            LOG.infof("Only host remains in room %s, ending matching", roomId);
            endMatching(roomId);
        }
    }

    public void updateFilters(String roomId, RoomFilters filters) {
        Room room = rooms.get(roomId);
        if (room != null) {
            room.setFilters(filters);
            LOG.infof("Updated filters for room %s: %s", roomId, filters);
        }
    }

    public void startMatching(String roomId) {
        Room room = rooms.get(roomId);
        if (room == null) {
            return;
        }

        room.start();
        broadcastService.broadcast(roomId, new ServerMessage.MatchingStarted());
        LOG.infof("Room %s started matching", roomId);

        // Start movie streaming in background
        streamMoviesInfinitely(roomId)
            .subscribe()
            .with(
                result ->
                    LOG.infof("Movie streaming completed for room %s", roomId),
                failure ->
                    LOG.errorf(
                        failure,
                        "Error streaming movies for room %s",
                        roomId
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
            LOG.infof(
                "Room %s is no longer active, stopping movie stream",
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
                    LOG.infof(
                        "Exhausted all pages for room %s, stopping stream",
                        roomId
                    );
                    broadcastService.broadcast(
                        roomId,
                        new ServerMessage.StreamingEnded()
                    );
                    return Uni.createFrom().voidItem();
                }

                if (newMovies.isEmpty()) {
                    LOG.infof(
                        "Page %d had no new movies, trying next page",
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

                LOG.infof(
                    "Fetched %d new movies from page %d for room %s, has_more: %b",
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
                            LOG.infof(
                                "No more pages available for room %s, stopping stream",
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
                LOG.errorf(
                    error,
                    "Error fetching movies for room %s on page %d",
                    roomId,
                    currentPage
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
        LOG.infof(
            "Participant %s liked movie %s in room %s",
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
            LOG.infof(
                "Match found! %d common movies in room %s",
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
        LOG.infof("Room %s ended matching", roomId);
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
