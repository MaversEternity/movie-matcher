package com.moviematcher.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviematcher.model.ClientMessage;
import com.moviematcher.service.RoomService;
import com.moviematcher.service.WebSocketBroadcastService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.jboss.logging.Logger;

@ServerEndpoint("/api/rooms/{roomId}/ws")
@ApplicationScoped
public class RoomWebSocket {

    private static final Logger LOG = Logger.getLogger(RoomWebSocket.class);

    @Inject
    RoomService roomService;

    @Inject
    WebSocketBroadcastService broadcastService;

    @Inject
    ObjectMapper objectMapper;

    @OnOpen
    public void onOpen(Session session, @PathParam("roomId") String roomId) {
        if (roomService.getRoom(roomId).isEmpty()) {
            LOG.errorf("Room %s not found for WebSocket connection", roomId);
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Room not found"));
            } catch (Exception e) {
                LOG.error("Error closing session", e);
            }
            return;
        }

        broadcastService.registerSession(roomId, session);
        LOG.infof("WebSocket connection opened for room %s, session %s", roomId, session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("roomId") String roomId) {
        try {
            ClientMessage clientMessage = objectMapper.readValue(message, ClientMessage.class);

            switch (clientMessage) {
                case ClientMessage.MovieLiked liked -> {
                    LOG.infof("Participant %s liked movie %s", liked.participantId(), liked.imdbId());
                    roomService.handleMovieLiked(roomId, liked.participantId(), liked.imdbId());
                }
                case ClientMessage.EndMatching endMatching -> {
                    LOG.infof("Manual end matching requested for room %s", roomId);
                    roomService.endMatching(roomId);
                }
                case ClientMessage.LeaveRoom leaveRoom -> {
                    LOG.infof("Participant %s leaving room %s", leaveRoom.participantId(), roomId);
                    roomService.leaveRoom(roomId, leaveRoom.participantId());
                    try {
                        session.close();
                    } catch (Exception e) {
                        LOG.error("Error closing session", e);
                    }
                }
            }
        } catch (Exception e) {
            LOG.errorf(e, "Error processing message: %s", message);
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("roomId") String roomId) {
        broadcastService.unregisterSession(roomId, session);
        LOG.infof("WebSocket connection closed for room %s, session %s", roomId, session.getId());
    }

    @OnError
    public void onError(Session session, @PathParam("roomId") String roomId, Throwable throwable) {
        LOG.errorf(throwable, "WebSocket error for room %s, session %s", roomId, session.getId());
    }
}
