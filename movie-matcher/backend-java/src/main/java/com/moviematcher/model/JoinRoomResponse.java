package com.moviematcher.model;

public record JoinRoomResponse(boolean success, String message, RoomInfo room) {
    // Конструктор для ошибок (без RoomInfo)
    public JoinRoomResponse(boolean success, String message) {
        this(success, message, null);
    }
}
