import 'dart:async';
import 'dart:convert';
import 'package:web_socket_channel/web_socket_channel.dart';
import '../models/client_message.dart';
import '../models/server_message.dart';

/// WebSocket сервис для real-time коммуникации с комнатой
class WebSocketService {
  WebSocketChannel? _channel;
  final _messageController = StreamController<ServerMessage>.broadcast();
  final _connectionController = StreamController<ConnectionState>.broadcast();
  String? _currentRoomId;

  /// Поток серверных сообщений
  Stream<ServerMessage> get messages => _messageController.stream;

  /// Поток состояния соединения
  Stream<ConnectionState> get connectionState => _connectionController.stream;

  /// Текущая комната
  String? get currentRoomId => _currentRoomId;

  /// Подключиться к комнате через WebSocket
  /// Эндпоинт: ws://host:3000/api/rooms/{roomId}/ws
  void connect(String roomId, {String? baseUrl}) {
    disconnect();

    _currentRoomId = roomId;
    final wsUrl = baseUrl ?? 'ws://10.0.2.2:3000';
    final uri = Uri.parse('$wsUrl/api/rooms/$roomId/ws');

    _connectionController.add(ConnectionState.connecting);

    try {
      _channel = WebSocketChannel.connect(uri);

      _channel!.stream.listen(
        (data) {
          try {
            final json = jsonDecode(data as String) as Map<String, dynamic>;
            final message = ServerMessage.fromJson(json);
            _messageController.add(message);
          } catch (e) {
            print('Ошибка парсинга WS-сообщения: $e');
          }
        },
        onError: (error) {
          print('Ошибка WebSocket: $error');
          _connectionController.add(ConnectionState.error);
        },
        onDone: () {
          _connectionController.add(ConnectionState.disconnected);
          _currentRoomId = null;
        },
      );

      _connectionController.add(ConnectionState.connected);
    } catch (e) {
      print('Ошибка подключения WebSocket: $e');
      _connectionController.add(ConnectionState.error);
    }
  }

  /// Отправить сообщение на сервер
  void send(ClientMessage message) {
    if (_channel == null) {
      print('WebSocket не подключён');
      return;
    }
    final json = jsonEncode(message.toJson());
    _channel!.sink.add(json);
  }

  /// Отключиться
  void disconnect() {
    _channel?.sink.close();
    _channel = null;
    _currentRoomId = null;
  }

  /// Освободить ресурсы
  void dispose() {
    disconnect();
    _messageController.close();
    _connectionController.close();
  }
}

/// Состояния WebSocket-соединения
enum ConnectionState {
  disconnected,
  connecting,
  connected,
  error,
}
