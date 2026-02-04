/// Клиентские сообщения для WebSocket — соответствие ClientMessage из backend-java
/// Каждое сообщение сериализуется в JSON с полем "type" для дискриминации на сервере
abstract class ClientMessage {
  Map<String, dynamic> toJson();
}

/// Установить фильтры для подбора фильмов
class SetFiltersMessage extends ClientMessage {
  final String participantId;
  final String? genre;
  final int? yearFrom;
  final int? yearTo;
  final double? minRating;
  final String? type;

  SetFiltersMessage({
    required this.participantId,
    this.genre,
    this.yearFrom,
    this.yearTo,
    this.minRating,
    this.type,
  });

  @override
  Map<String, dynamic> toJson() {
    // Бэкенд использует Jackson @JsonTypeInfo(property = "type") для дискриминатора.
    // Поле "type" в SetFilters — это фильм/сериал, Jackson разделяет их автоматически.
    final map = <String, dynamic>{
      'type': 'SetFilters',
      'participant_id': participantId,
    };
    if (genre != null) map['genre'] = genre;
    if (yearFrom != null) map['year_from'] = yearFrom;
    if (yearTo != null) map['year_to'] = yearTo;
    if (minRating != null) map['min_rating'] = minRating;
    return map;
  }
}

/// Поиск фильма по названию
class SearchMovieMessage extends ClientMessage {
  final String participantId;
  final String query;

  SearchMovieMessage({required this.participantId, required this.query});

  @override
  Map<String, dynamic> toJson() => {
        'type': 'SearchMovie',
        'participant_id': participantId,
        'query': query,
      };
}

/// Добавить найденный фильм в выборку
class AddMovieToSelectionMessage extends ClientMessage {
  final String participantId;
  final String movieId;

  AddMovieToSelectionMessage({
    required this.participantId,
    required this.movieId,
  });

  @override
  Map<String, dynamic> toJson() => {
        'type': 'AddMovieToSelection',
        'participant_id': participantId,
        'movie_id': movieId,
      };
}

/// Участник готов к голосованию
class ReadyToVoteMessage extends ClientMessage {
  final String participantId;

  ReadyToVoteMessage({required this.participantId});

  @override
  Map<String, dynamic> toJson() => {
        'type': 'ReadyToVote',
        'participant_id': participantId,
      };
}

/// Голосование за фильм (свайп вверх/вниз)
class VoteMessage extends ClientMessage {
  final String participantId;
  final String movieId;
  final bool isLike;

  VoteMessage({
    required this.participantId,
    required this.movieId,
    required this.isLike,
  });

  @override
  Map<String, dynamic> toJson() => {
        'type': 'Vote',
        'participant_id': participantId,
        'movie_id': movieId,
        'is_like': isLike,
      };
}

/// Выйти из комнаты
class LeaveRoomMessage extends ClientMessage {
  final String participantId;

  LeaveRoomMessage({required this.participantId});

  @override
  Map<String, dynamic> toJson() => {
        'type': 'LeaveRoom',
        'participant_id': participantId,
      };
}
