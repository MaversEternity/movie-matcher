/// DTO информации о комнате — соответствие RoomInfo из backend-java
class RoomInfo {
  final String id;
  final RoomFilters? filters;
  final int participantsCount;
  final bool isActive;

  const RoomInfo({
    required this.id,
    this.filters,
    required this.participantsCount,
    required this.isActive,
  });

  factory RoomInfo.fromJson(Map<String, dynamic> json) {
    return RoomInfo(
      id: json['id'] as String,
      filters: json['filters'] != null
          ? RoomFilters.fromJson(json['filters'] as Map<String, dynamic>)
          : null,
      participantsCount: json['participants_count'] as int? ?? 0,
      isActive: json['is_active'] as bool? ?? false,
    );
  }
}

/// Фильтры комнаты — соответствие RoomFilters / MovieFilters из backend-java
class RoomFilters {
  final String? genre;
  final int? yearFrom;
  final int? yearTo;
  final double? minRating;
  final String type;

  const RoomFilters({
    this.genre,
    this.yearFrom,
    this.yearTo,
    this.minRating,
    this.type = 'movie',
  });

  factory RoomFilters.fromJson(Map<String, dynamic> json) {
    return RoomFilters(
      genre: json['genre'] as String?,
      yearFrom: json['yearFrom'] as int? ?? json['year_from'] as int?,
      yearTo: json['yearTo'] as int? ?? json['year_to'] as int?,
      minRating: (json['minRating'] ?? json['min_rating'])?.toDouble(),
      type: json['type'] as String? ?? 'movie',
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'genre': genre,
      'year_from': yearFrom,
      'year_to': yearTo,
      'min_rating': minRating,
      'type': type,
    };
  }

  bool get hasAnyFilter =>
      genre != null || yearFrom != null || minRating != null;
}
