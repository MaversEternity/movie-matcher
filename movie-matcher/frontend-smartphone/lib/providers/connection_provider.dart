import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../services/api_service.dart';
import '../services/websocket_service.dart';

/// Провайдер API-сервиса (singleton)
final apiServiceProvider = Provider<ApiService>((ref) {
  return ApiService();
});

/// Провайдер WebSocket-сервиса (singleton)
final wsServiceProvider = Provider<WebSocketService>((ref) {
  final service = WebSocketService();
  ref.onDispose(() => service.dispose());
  return service;
});

/// Провайдер состояния WS-соединения
final connectionStateProvider =
    StreamProvider<ConnectionState>((ref) {
  final ws = ref.watch(wsServiceProvider);
  return ws.connectionState;
});
