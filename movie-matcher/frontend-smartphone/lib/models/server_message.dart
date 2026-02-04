import 'movie_data.dart';
import 'participant_likes.dart';

/// Серверные сообщения из WebSocket — соответствие ServerMessage из backend-java
/// Десериализация по полю "type"
sealed class ServerMessage {
  const ServerMessage();

  factory ServerMessage.fromJson(Map<String, dynamic> json) {
    final type = json['type'] as String;
    switch (type) {
      case 'ParticipantJoined':
        return ParticipantJoinedMessage(
          participantId: json['participant_id'] as String,
        );
      case 'ParticipantLeft':
        return ParticipantLeftMessage(
          participantId: json['participant_id'] as String,
        );
      case 'ParticipantReady':
        return ParticipantReadyMessage(
          participantId: json['participant_id'] as String,
          readyCount: json['ready_count'] as int,
          totalCount: json['total_count'] as int,
        );
      case 'VotingStarted':
        return const VotingStartedMessage();
      case 'MatchingStarted':
        return const MatchingStartedMessage();
      case 'RoomLocked':
        return RoomLockedMessage(message: json['message'] as String);
      case 'NewMovie':
        return NewMovieMessage(
          movie: MovieData.fromJson(json['movie'] as Map<String, dynamic>),
        );
      case 'VoteRecorded':
        return VoteRecordedMessage(
          participantId: json['participant_id'] as String,
          movieId: json['movie_id'] as String,
          isLike: json['is_like'] as bool,
        );
      case 'RoundCompleted':
        return RoundCompletedMessage(
          roundNumber: json['round_number'] as int,
          commonLikes: (json['common_likes'] as List<dynamic>)
              .map((e) => e as String)
              .toList(),
        );
      case 'VotingCompleted':
        return VotingCompletedMessage(
          matchedMovies: (json['matched_movies'] as List<dynamic>)
              .map((e) => e as String)
              .toList(),
        );
      case 'LikesUpdated':
        return LikesUpdatedMessage(
          allLikes: (json['all_likes'] as List<dynamic>)
              .map((e) =>
                  ParticipantLikes.fromJson(e as Map<String, dynamic>))
              .toList(),
          commonLikes: (json['common_likes'] as List<dynamic>)
              .map((e) => MovieData.fromJson(e as Map<String, dynamic>))
              .toList(),
        );
      case 'MatchingEnded':
        return MatchingEndedMessage(
          allLikes: (json['all_likes'] as List<dynamic>)
              .map((e) =>
                  ParticipantLikes.fromJson(e as Map<String, dynamic>))
              .toList(),
          commonLikes: (json['common_likes'] as List<dynamic>)
              .map((e) => MovieData.fromJson(e as Map<String, dynamic>))
              .toList(),
        );
      case 'MatchFound':
        return MatchFoundMessage(
          allLikes: (json['all_likes'] as List<dynamic>)
              .map((e) =>
                  ParticipantLikes.fromJson(e as Map<String, dynamic>))
              .toList(),
          commonLikes: (json['common_likes'] as List<dynamic>)
              .map((e) => MovieData.fromJson(e as Map<String, dynamic>))
              .toList(),
        );
      case 'StreamingEnded':
        return const StreamingEndedMessage();
      case 'NoMoreMovies':
        return const NoMoreMoviesMessage();
      case 'Error':
        return ErrorMessage(message: json['message'] as String);
      default:
        return ErrorMessage(message: 'Неизвестный тип сообщения: $type');
    }
  }
}

class ParticipantJoinedMessage extends ServerMessage {
  final String participantId;
  const ParticipantJoinedMessage({required this.participantId});
}

class ParticipantLeftMessage extends ServerMessage {
  final String participantId;
  const ParticipantLeftMessage({required this.participantId});
}

class ParticipantReadyMessage extends ServerMessage {
  final String participantId;
  final int readyCount;
  final int totalCount;
  const ParticipantReadyMessage({
    required this.participantId,
    required this.readyCount,
    required this.totalCount,
  });
}

class VotingStartedMessage extends ServerMessage {
  const VotingStartedMessage();
}

class MatchingStartedMessage extends ServerMessage {
  const MatchingStartedMessage();
}

class RoomLockedMessage extends ServerMessage {
  final String message;
  const RoomLockedMessage({required this.message});
}

class NewMovieMessage extends ServerMessage {
  final MovieData movie;
  const NewMovieMessage({required this.movie});
}

class VoteRecordedMessage extends ServerMessage {
  final String participantId;
  final String movieId;
  final bool isLike;
  const VoteRecordedMessage({
    required this.participantId,
    required this.movieId,
    required this.isLike,
  });
}

class RoundCompletedMessage extends ServerMessage {
  final int roundNumber;
  final List<String> commonLikes;
  const RoundCompletedMessage({
    required this.roundNumber,
    required this.commonLikes,
  });
}

class VotingCompletedMessage extends ServerMessage {
  final List<String> matchedMovies;
  const VotingCompletedMessage({required this.matchedMovies});
}

class LikesUpdatedMessage extends ServerMessage {
  final List<ParticipantLikes> allLikes;
  final List<MovieData> commonLikes;
  const LikesUpdatedMessage({
    required this.allLikes,
    required this.commonLikes,
  });
}

class MatchingEndedMessage extends ServerMessage {
  final List<ParticipantLikes> allLikes;
  final List<MovieData> commonLikes;
  const MatchingEndedMessage({
    required this.allLikes,
    required this.commonLikes,
  });
}

class MatchFoundMessage extends ServerMessage {
  final List<ParticipantLikes> allLikes;
  final List<MovieData> commonLikes;
  const MatchFoundMessage({
    required this.allLikes,
    required this.commonLikes,
  });
}

class StreamingEndedMessage extends ServerMessage {
  const StreamingEndedMessage();
}

class NoMoreMoviesMessage extends ServerMessage {
  const NoMoreMoviesMessage();
}

class ErrorMessage extends ServerMessage {
  final String message;
  const ErrorMessage({required this.message});
}
