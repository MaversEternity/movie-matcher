package com.moviematcher.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.Getter;
import lombok.Setter;

@Getter
public class Room {

    private final String id;

    @Setter
    private RoomFilters filters;

    private final List<String> participants = new CopyOnWriteArrayList<>();

    @Setter
    private volatile boolean isActive = false;

    @JsonProperty("host_id")
    private final String hostId;

    @JsonIgnore
    private final Map<String, List<String>> participantLikes =
        new ConcurrentHashMap<>();

    @JsonIgnore
    private final Set<String> sentMovieIds = ConcurrentHashMap.newKeySet();

    @JsonIgnore
    private volatile int currentPage = 1;

    public Room(String id, RoomFilters filters, String hostId) {
        this.id = id;
        this.filters = filters;
        this.hostId = hostId;
        this.participants.add(hostId);
        this.participantLikes.put(hostId, new CopyOnWriteArrayList<>());
    }

    // Return defensive copy for participants
    public List<String> getParticipants() {
        return new ArrayList<>(participants);
    }

    public void incrementPage() {
        currentPage++;
    }

    public boolean addParticipant(String participantId) {
        if (!participants.contains(participantId)) {
            participants.add(participantId);
            participantLikes.put(participantId, new CopyOnWriteArrayList<>());
            return true;
        }
        return false;
    }

    public void removeParticipant(String participantId) {
        participants.remove(participantId);
        participantLikes.remove(participantId);
    }

    public void start() {
        this.isActive = true;
        participantLikes.values().forEach(List::clear);
        sentMovieIds.clear();
        currentPage = 1;
    }

    public void addLike(String participantId, String imdbId) {
        List<String> likes = participantLikes.get(participantId);
        if (likes != null && !likes.contains(imdbId)) {
            likes.add(imdbId);
        }
    }

    public boolean hasMovieBeenSent(String imdbId) {
        return sentMovieIds.contains(imdbId);
    }

    public void markMovieAsSent(String imdbId) {
        sentMovieIds.add(imdbId);
    }

    public List<MovieData> getCommonLikes(List<MovieData> allMovies) {
        if (participants.size() < 2) {
            return List.of();
        }

        Map<String, Integer> movieCounts = new HashMap<>();

        for (List<String> likes : participantLikes.values()) {
            Set<String> uniqueLikes = new HashSet<>(likes);
            for (String imdbId : uniqueLikes) {
                movieCounts.merge(imdbId, 1, Integer::sum);
            }
        }

        int numParticipants = participants.size();
        Set<String> commonMovieIds = movieCounts
            .entrySet()
            .stream()
            .filter(e -> e.getValue() == numParticipants)
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toSet());

        return allMovies
            .stream()
            .filter(m -> commonMovieIds.contains(m.imdbId()))
            .toList();
    }

    public List<ParticipantLikes> getAllParticipantLikes(
        List<MovieData> allMovies
    ) {
        return participants
            .stream()
            .map(participantId -> {
                List<String> likedMovieIds = participantLikes.getOrDefault(
                    participantId,
                    List.of()
                );
                List<MovieData> likedMovies = allMovies
                    .stream()
                    .filter(m -> likedMovieIds.contains(m.imdbId()))
                    .toList();
                return new ParticipantLikes(participantId, likedMovies);
            })
            .toList();
    }

    public Map<String, List<String>> getParticipantLikesMap() {
        return new HashMap<>(participantLikes);
    }
}
