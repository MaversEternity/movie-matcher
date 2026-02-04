import 'movie_data.dart';

/// DTO лайков участника — соответствие ParticipantLikes из backend-java
class ParticipantLikes {
  final String participantId;
  final List<MovieData> likedMovies;

  const ParticipantLikes({
    required this.participantId,
    required this.likedMovies,
  });

  factory ParticipantLikes.fromJson(Map<String, dynamic> json) {
    return ParticipantLikes(
      participantId: json['participant_id'] as String,
      likedMovies: (json['liked_movies'] as List<dynamic>?)
              ?.map((e) => MovieData.fromJson(e as Map<String, dynamic>))
              .toList() ??
          [],
    );
  }
}
