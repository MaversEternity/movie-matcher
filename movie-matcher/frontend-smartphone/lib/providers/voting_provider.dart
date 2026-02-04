import 'dart:async';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models/movie_data.dart';
import '../models/participant_likes.dart';
import '../models/server_message.dart';
import '../services/websocket_service.dart';
import 'connection_provider.dart';

/// Состояние голосования
class VotingState {
  /// Очередь фильмов для голосования
  final List<MovieData> movieQueue;

  /// Текущий номер раунда
  final int currentRound;

  /// Общие лайки за раунд (ID фильмов)
  final List<String> commonLikeIds;

  /// Совпавшие фильмы (полные данные)
  final List<MovieData> matchedMovies;

  /// Все лайки всех участников
  final List<ParticipantLikes> allLikes;

  /// Голосование завершено
  final bool isCompleted;

  /// Нет больше фильмов
  final bool noMoreMovies;

  /// Ожидание других участников
  final bool waitingForOthers;

  const VotingState({
    this.movieQueue = const [],
    this.currentRound = 0,
    this.commonLikeIds = const [],
    this.matchedMovies = const [],
    this.allLikes = const [],
    this.isCompleted = false,
    this.noMoreMovies = false,
    this.waitingForOthers = false,
  });

  VotingState copyWith({
    List<MovieData>? movieQueue,
    int? currentRound,
    List<String>? commonLikeIds,
    List<MovieData>? matchedMovies,
    List<ParticipantLikes>? allLikes,
    bool? isCompleted,
    bool? noMoreMovies,
    bool? waitingForOthers,
  }) {
    return VotingState(
      movieQueue: movieQueue ?? this.movieQueue,
      currentRound: currentRound ?? this.currentRound,
      commonLikeIds: commonLikeIds ?? this.commonLikeIds,
      matchedMovies: matchedMovies ?? this.matchedMovies,
      allLikes: allLikes ?? this.allLikes,
      isCompleted: isCompleted ?? this.isCompleted,
      noMoreMovies: noMoreMovies ?? this.noMoreMovies,
      waitingForOthers: waitingForOthers ?? this.waitingForOthers,
    );
  }

  /// Текущий фильм для голосования (первый в очереди)
  MovieData? get currentMovie =>
      movieQueue.isNotEmpty ? movieQueue.first : null;
}

/// Провайдер состояния голосования
class VotingNotifier extends StateNotifier<VotingState> {
  final WebSocketService _ws;
  StreamSubscription<ServerMessage>? _subscription;

  VotingNotifier(this._ws) : super(const VotingState()) {
    _subscription = _ws.messages.listen(_handleMessage);
  }

  /// Удалить текущий фильм из очереди (после свайпа)
  void removeCurrentMovie() {
    if (state.movieQueue.isNotEmpty) {
      final queue = [...state.movieQueue]..removeAt(0);
      state = state.copyWith(
        movieQueue: queue,
        waitingForOthers: queue.isEmpty,
      );
    }
  }

  /// Сбросить состояние голосования
  void reset() {
    state = const VotingState();
  }

  void _handleMessage(ServerMessage message) {
    switch (message) {
      case NewMovieMessage msg:
        // Добавить фильм в очередь голосования
        final queue = [...state.movieQueue, msg.movie];
        state = state.copyWith(
          movieQueue: queue,
          waitingForOthers: false,
        );

      case RoundCompletedMessage msg:
        state = state.copyWith(
          currentRound: msg.roundNumber,
          commonLikeIds: msg.commonLikes,
        );

      case VotingCompletedMessage _:
        state = state.copyWith(isCompleted: true);

      case MatchFoundMessage msg:
        state = state.copyWith(
          matchedMovies: msg.commonLikes,
          allLikes: msg.allLikes,
          isCompleted: true,
        );

      case MatchingEndedMessage msg:
        state = state.copyWith(
          matchedMovies: msg.commonLikes,
          allLikes: msg.allLikes,
          isCompleted: true,
        );

      case LikesUpdatedMessage msg:
        state = state.copyWith(
          allLikes: msg.allLikes,
          matchedMovies: msg.commonLikes,
        );

      case NoMoreMoviesMessage _:
        state = state.copyWith(noMoreMovies: true);

      case StreamingEndedMessage _:
        state = state.copyWith(noMoreMovies: true);

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

final votingProvider =
    StateNotifierProvider<VotingNotifier, VotingState>((ref) {
  final ws = ref.watch(wsServiceProvider);
  return VotingNotifier(ws);
});
