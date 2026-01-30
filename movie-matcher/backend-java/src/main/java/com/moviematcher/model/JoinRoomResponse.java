package com.moviematcher.model;

public record JoinRoomResponse(
    boolean success,
    RoomInfo room
) {}
