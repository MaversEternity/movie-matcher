package com.moviematcher.resource;

import com.moviematcher.service.RoomApplicationService;
import com.moviematcher.domain.model.VotingCompletionType;
import com.moviematcher.model.*;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

/**
 * REST API для управления комнатами
 */
@Path("/api/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private static final Logger log = Logger.getLogger(RoomResource.class);

    private final RoomApplicationService roomService;

    @jakarta.inject.Inject
    public RoomResource(RoomApplicationService roomService) {
        this.roomService = roomService;
    }

    /**
     * Создать комнату
     * POST /api/rooms
     */
    @POST
    public Response createRoom(@Valid CreateRoomRequest request) {
        log.infof("Creating room for host: {}", request.hostId());

        VotingCompletionType completionType = VotingCompletionType.UNANIMOUS;

        CreateRoomResponse response = roomService.createRoom(
            request.hostId(),
            completionType
        );

        return Response.ok(response).build();
    }

    /**
     * Получить информацию о комнате
     * GET /api/rooms/{roomId}
     */
    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        log.debugf("Getting room info: {}", roomId);

        var roomInfo = roomService.getRoomInfo(roomId);

        if (roomInfo.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Room not found"))
                .build();
        }

        return Response.ok(roomInfo.get()).build();
    }

    /**
     * Присоединиться к комнате
     * POST /api/rooms/{roomId}/join
     */
    @POST
    @Path("/{roomId}/join")
    public Response joinRoom(
        @PathParam("roomId") String roomId,
        @Valid JoinRoomRequest request
    ) {
        log.infof(
            "Participant {} joining room {}",
            request.participantId(),
            roomId
        );

        JoinRoomResponse response = roomService.joinRoom(
            roomId,
            request.participantId()
        );

        if (!response.success()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(response)
                .build();
        }

        return Response.ok(response).build();
    }

    /**
     * Покинуть комнату
     * POST /api/rooms/{roomId}/leave
     */
    @POST
    @Path("/{roomId}/leave")
    public Response leaveRoom(
        @PathParam("roomId") String roomId,
        LeaveRoomRequest request
    ) {
        log.infof(
            "Participant {} leaving room {}",
            request.participantId(),
            roomId
        );

        roomService.leaveRoom(roomId, request.participantId());

        return Response.ok().build();
    }

    /**
     * Начать голосование
     * POST /api/rooms/{roomId}/start
     */
    @POST
    @Path("/{roomId}/start")
    public Response startVoting(@PathParam("roomId") String roomId) {
        log.infof("Starting voting in room {}", roomId);

        try {
            roomService.startVoting(roomId);
            return Response.ok().build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(e.getMessage()))
                .build();
        } catch (IllegalArgumentException e) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Room not found"))
                .build();
        }
    }

    // Helper records
    record ErrorResponse(String message) {}

    record LeaveRoomRequest(String participantId) {}
}
