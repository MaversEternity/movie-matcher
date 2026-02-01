package com.moviematcher.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviematcher.model.ServerMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jboss.logging.Logger;

@ApplicationScoped
public class WebSocketBroadcastService {

    private static final Logger log = Logger.getLogger(
        WebSocketBroadcastService.class
    );

    private final Map<String, Set<Session>> roomSessions =
        new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    @jakarta.inject.Inject
    public WebSocketBroadcastService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void registerSession(String roomId, Session session) {
        roomSessions
            .computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet())
            .add(session);
        log.infof("Session {} registered to room {}", session.getId(), roomId);
    }

    public void unregisterSession(String roomId, Session session) {
        Set<Session> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomId);
            }
        }
        log.infof(
            "Session {} unregistered from room {}",
            session.getId(),
            roomId
        );
    }

    public void broadcast(String roomId, ServerMessage message) {
        Set<Session> sessions = roomSessions.get(roomId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(message);

            // Remove closed sessions
            sessions.removeIf(session -> !session.isOpen());

            for (Session session : sessions) {
                if (session.isOpen()) {
                    session.getAsyncRemote().sendText(json);
                }
            }
        } catch (JsonProcessingException e) {
            log.errorf("Error serializing message: {}", message, e);
        }
    }

    public int getSessionCount(String roomId) {
        Set<Session> sessions = roomSessions.get(roomId);
        return sessions != null ? sessions.size() : 0;
    }
}
