import 'dart:async';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/movie_data.dart';
import '../models/room_info.dart';
import '../models/server_message.dart';
import '../services/websocket_service.dart';
import 'connection_provider.dart';

/// Состояние комнаты
class RoomState {
  final String? roomId;
  final String? participantId;
  final bool isHost;
  final String? joinUrl;
  final int participantsCount;
  final int readyCount;
  final int totalCount;
  final RoomFilters? filters;
  final List<MovieData> searchResults;
  final List<String> selectedMovieIds;
  final bool isReady;
  final RoomStatus status;
  final String? error;

  const RoomState({
    this.roomId,
    this.participantId,
    this.isHost = false,
    this.joinUrl,
    this.participantsCount = 0,
    this.readyCount = 0,
    this.totalCount = 0,
    this.filters,
    this.searchResults = const [],
    this.selectedMovieIds = const [],
    this.isReady = false,
    this.status = RoomStatus.idle,
    this.error,
  });

  RoomState copyWith({
    String? roomId,
    String? participantId,
    bool? isHost,
    String? joinUrl,
    int? participantsCount,
    int? readyCount,
    int? totalCount,
    RoomFilters? filters,
    List<MovieData>? searchResults,
    List<String>? selectedMovieIds,
    bool? isReady,
    RoomStatus? status,
    String? error,
  }) {
    return RoomState(
      roomId: roomId ?? this.roomId,
      participantId: participantId ?? this.participantId,
      isHost: isHost ?? this.isHost,
      joinUrl: joinUrl ?? this.joinUrl,
      participantsCount: participantsCount ?? this.participantsCount,
      readyCount: readyCount ?? this.readyCount,
      totalCount: totalCount ?? this.totalCount,
      filters: filters ?? this.filters,
      searchResults: searchResults ?? this.searchResults,
      selectedMovieIds: selectedMovieIds ?? this.selectedMovieIds,
      isReady: isReady ?? this.isReady,
      status: status ?? this.status,
      error: error,
    );
  }
}

enum RoomStatus { idle, lobby, voting, results }

/// Провайдер состояния комнаты
class RoomNotifier extends StateNotifier<RoomState> {
  final WebSocketService _ws;
  StreamSubscription<ServerMessage>? _subscription;

  RoomNotifier(this._ws) : super(const RoomState()) {
    _subscription = _ws.messages.listen(_handleMessage);
  }

  /// Инициализировать комнату после создания
  void initAsHost({
    required String roomId,
    required String participantId,
    required String joinUrl,
  }) {
    state = state.copyWith(
      roomId: roomId,
      participantId: participantId,
      isHost: true,
      joinUrl: joinUrl,
      participantsCount: 1,
      status: RoomStatus.lobby,
    );
    _ws.connect(roomId);
  }

  /// Инициализировать комнату после присоединения
  void initAsParticipant({
    required String roomId,
    required String participantId,
    required int participantsCount,
  }) {
    state = state.copyWith(
      roomId: roomId,
      participantId: participantId,
      isHost: false,
      participantsCount: participantsCount,
      status: RoomStatus.lobby,
    );
    _ws.connect(roomId);
  }

  /// Обновить фильтры
  void setFilters(RoomFilters filters) {
    state = state.copyWith(filters: filters);
  }

  /// Добавить результат поиска
  void addSearchResult(MovieData movie) {
    final results = [...state.searchResults, movie];
    state = state.copyWith(searchResults: results);
  }

  /// Очистить результаты поиска
  void clearSearchResults() {
    state = state.copyWith(searchResults: []);
  }

  /// Добавить фильм в выборку
  void addSelectedMovie(String movieId) {
    if (!state.selectedMovieIds.contains(movieId)) {
      final ids = [...state.selectedMovieIds, movieId];
      state = state.copyWith(selectedMovieIds: ids);
    }
  }

  /// Отметить готовность
  void markReady() {
    state = state.copyWith(isReady: true);
  }

  /// Перейти к голосованию
  void startVoting() {
    state = state.copyWith(status: RoomStatus.voting);
  }

  /// Перейти к результатам
  void showResults() {
    state = state.copyWith(status: RoomStatus.results);
  }

  /// Сбросить состояние
  void reset() {
    _ws.disconnect();
    state = const RoomState();
  }

  /// Обработка входящих серверных сообщений
  void _handleMessage(ServerMessage message) {
    switch (message) {
      case ParticipantJoinedMessage msg:
        state = state.copyWith(
          participantsCount: state.participantsCount + 1,
        );
      case ParticipantLeftMessage msg:
        state = state.copyWith(
          participantsCount:
              (state.participantsCount - 1).clamp(0, double.maxFinite.toInt()),
        );
      case ParticipantReadyMessage msg:
        state = state.copyWith(
          readyCount: msg.readyCount,
          totalCount: msg.totalCount,
        );
      case VotingStartedMessage _:
        state = state.copyWith(status: RoomStatus.voting);
      case RoomLockedMessage msg:
        state = state.copyWith(error: msg.message);
      case ErrorMessage msg:
        state = state.copyWith(error: msg.message);
      case NewMovieMessage msg:
        addSearchResult(msg.movie);
      default:
        break;
    }
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }
}

final roomProvider = StateNotifierProvider<RoomNotifier, RoomState>((ref) {
  final ws = ref.watch(wsServiceProvider);
  return RoomNotifier(ws);
});
