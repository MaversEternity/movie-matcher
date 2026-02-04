import 'package:dio/dio.dart';
import '../models/room_info.dart';

/// REST API сервис для взаимодействия с backend-java
class ApiService {
  final Dio _dio;

  ApiService({String? baseUrl})
      : _dio = Dio(BaseOptions(
          baseUrl: baseUrl ?? 'http://10.0.2.2:3000',
          connectTimeout: const Duration(seconds: 10),
          receiveTimeout: const Duration(seconds: 10),
          headers: {'Content-Type': 'application/json'},
        ));

  /// Создать комнату
  /// POST /api/rooms
  Future<CreateRoomResult> createRoom(String hostId) async {
    final response = await _dio.post('/api/rooms', data: {
      'host_id': hostId,
      'filters': {
        'genre': null,
        'year_from': null,
        'year_to': null,
        'min_rating': null,
        'type': 'movie',
      },
    });
    final data = response.data as Map<String, dynamic>;
    return CreateRoomResult(
      roomId: data['room_id'] as String,
      joinUrl: data['join_url'] as String,
    );
  }

  /// Получить информацию о комнате
  /// GET /api/rooms/{roomId}
  Future<RoomInfo> getRoomInfo(String roomId) async {
    final response = await _dio.get('/api/rooms/$roomId');
    return RoomInfo.fromJson(response.data as Map<String, dynamic>);
  }

  /// Присоединиться к комнате
  /// POST /api/rooms/{roomId}/join
  Future<JoinRoomResult> joinRoom(String roomId, String participantId) async {
    final response = await _dio.post('/api/rooms/$roomId/join', data: {
      'participant_id': participantId,
    });
    final data = response.data as Map<String, dynamic>;
    return JoinRoomResult(
      success: data['success'] as bool,
      message: data['message'] as String? ?? '',
      room: data['room'] != null
          ? RoomInfo.fromJson(data['room'] as Map<String, dynamic>)
          : null,
    );
  }

  /// Покинуть комнату
  /// POST /api/rooms/{roomId}/leave
  Future<void> leaveRoom(String roomId, String participantId) async {
    await _dio.post('/api/rooms/$roomId/leave', data: {
      'participantId': participantId,
    });
  }

  /// Начать голосование (только хост)
  /// POST /api/rooms/{roomId}/start
  Future<void> startVoting(String roomId) async {
    await _dio.post('/api/rooms/$roomId/start');
  }
}

class CreateRoomResult {
  final String roomId;
  final String joinUrl;

  const CreateRoomResult({required this.roomId, required this.joinUrl});
}

class JoinRoomResult {
  final bool success;
  final String message;
  final RoomInfo? room;

  const JoinRoomResult({
    required this.success,
    required this.message,
    this.room,
  });
}
