package com.moviematcher.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviematcher.model.ServerMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.Session;
import org.jboss.logging.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class WebSocketBroadcastService {

    private static final Logger LOG = Logger.getLogger(WebSocketBroadcastService.class);

    private final Map<String, Set<Session>> roomSessions = new ConcurrentHashMap<>();

    @Inject
    ObjectMapper objectMapper;

    public void registerSession(String roomId, Session session) {
        roomSessions.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(session);
        LOG.infof("Session %s registered to room %s", session.getId(), roomId);
    }

    public void unregisterSession(String roomId, Session session) {
        Set<Session> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                roomSessions.remove(roomId);
            }
        }
        LOG.infof("Session %s unregistered from room %s", session.getId(), roomId);
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
            LOG.errorf(e, "Error serializing message: %s", message);
        }
    }

    public int getSessionCount(String roomId) {
        Set<Session> sessions = roomSessions.get(roomId);
        return sessions != null ? sessions.size() : 0;
    }
}
