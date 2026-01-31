package com.moviematcher.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moviematcher.model.ClientMessage;
import com.moviematcher.service.RoomService;
import com.moviematcher.service.WebSocketBroadcastService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@ServerEndpoint("/api/rooms/{roomId}/ws")
@ApplicationScoped
public class RoomWebSocket {

    private final RoomService roomService;
    private final WebSocketBroadcastService broadcastService;
    private final ObjectMapper objectMapper;

    @OnOpen
    public void onOpen(Session session, @PathParam("roomId") String roomId) {
        if (roomService.getRoom(roomId).isEmpty()) {
            log.error("Room {} not found for WebSocket connection", roomId);
            try {
                session.close(
                    new CloseReason(
                        CloseReason.CloseCodes.CANNOT_ACCEPT,
                        "Room not found"
                    )
                );
            } catch (Exception e) {
                log.error("Error closing session", e);
            }
            return;
        }

        broadcastService.registerSession(roomId, session);
        log.info(
            "WebSocket connection opened for room {}, session {}",
            roomId,
            session.getId()
        );
    }

    @OnMessage
    public void onMessage(
        String message,
        Session session,
        @PathParam("roomId") String roomId
    ) {
        try {
            ClientMessage clientMessage = objectMapper.readValue(
                message,
                ClientMessage.class
            );

            switch (clientMessage) {
                case ClientMessage.MovieLiked liked -> {
                    log.info(
                        "Participant {} liked movie {}",
                        liked.participantId(),
                        liked.imdbId()
                    );
                    roomService.handleMovieLiked(
                        roomId,
                        liked.participantId(),
                        liked.imdbId()
                    );
                }
                case ClientMessage.EndMatching endMatching -> {
                    log.info(
                        "Manual end matching requested for room {}",
                        roomId
                    );
                    roomService.endMatching(roomId);
                }
                case ClientMessage.LeaveRoom leaveRoom -> {
                    log.info(
                        "Participant {} leaving room {}",
                        leaveRoom.participantId(),
                        roomId
                    );
                    roomService.leaveRoom(roomId, leaveRoom.participantId());
                    try {
                        session.close();
                    } catch (Exception e) {
                        log.error("Error closing session", e);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing message: {}", message, e);
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("roomId") String roomId) {
        broadcastService.unregisterSession(roomId, session);
        log.info(
            "WebSocket connection closed for room {}, session {}",
            roomId,
            session.getId()
        );
    }

    @OnError
    public void onError(
        Session session,
        @PathParam("roomId") String roomId,
        Throwable throwable
    ) {
        log.error(
            "WebSocket error for room {}, session {}",
            roomId,
            session.getId(),
            throwable
        );
    }
}
