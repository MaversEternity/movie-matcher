package com.moviematcher.resource;

import com.moviematcher.model.*;
import com.moviematcher.service.RoomService;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Path("/api/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private final RoomService roomService;

    @POST
    public Response createRoom(@Valid CreateRoomRequest request) {
        Room room = roomService.createRoom(request.filters(), request.hostId());
        CreateRoomResponse response = new CreateRoomResponse(
            room.getId(),
            "/room/" + room.getId()
        );
        return Response.ok(response).build();
    }

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        RoomInfo roomInfo = roomService.getRoomInfo(roomId);
        if (roomInfo == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(roomInfo).build();
    }

    @GET
    @Path("/{roomId}/state")
    public Response getRoomState(@PathParam("roomId") String roomId) {
        RoomState roomState = roomService.getRoomState(roomId);
        if (roomState == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(roomState).build();
    }

    @POST
    @Path("/{roomId}/join")
    public Response joinRoom(
        @PathParam("roomId") String roomId,
        @Valid JoinRoomRequest request
    ) {
        boolean success = roomService.joinRoom(roomId, request.participantId());
        RoomInfo roomInfo = roomService.getRoomInfo(roomId);

        if (roomInfo == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        JoinRoomResponse response = new JoinRoomResponse(success, roomInfo);
        return Response.ok(response).build();
    }

    @PUT
    @Path("/{roomId}/filters")
    public Response updateFilters(
        @PathParam("roomId") String roomId,
        @Valid RoomFilters filters
    ) {
        if (roomService.getRoom(roomId).isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        roomService.updateFilters(roomId, filters);
        return Response.ok().build();
    }

    @POST
    @Path("/{roomId}/start")
    public Response startMatching(@PathParam("roomId") String roomId) {
        if (roomService.getRoom(roomId).isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        roomService.startMatching(roomId);
        return Response.ok().build();
    }
}
