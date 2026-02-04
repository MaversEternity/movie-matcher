/// DTO фильма — точное соответствие MovieData из backend-java
class MovieData {
  final String? title;
  final String? year;
  final String? rated;
  final String? runtime;
  final String? poster;
  final String? director;
  final String? actors;
  final String? plot;
  final String? country;
  final String? genre;
  final String? imdbRating;
  final String? imdbId;

  const MovieData({
    this.title,
    this.year,
    this.rated,
    this.runtime,
    this.poster,
    this.director,
    this.actors,
    this.plot,
    this.country,
    this.genre,
    this.imdbRating,
    this.imdbId,
  });

  factory MovieData.fromJson(Map<String, dynamic> json) {
    return MovieData(
      title: json['title'] as String?,
      year: json['year'] as String?,
      rated: json['rated'] as String?,
      runtime: json['runtime'] as String?,
      poster: json['poster'] as String?,
      director: json['director'] as String?,
      actors: json['actors'] as String?,
      plot: json['plot'] as String?,
      country: json['country'] as String?,
      genre: json['genre'] as String?,
      imdbRating: json['imdb_rating'] as String?,
      imdbId: json['imdb_id'] as String?,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'title': title,
      'year': year,
      'rated': rated,
      'runtime': runtime,
      'poster': poster,
      'director': director,
      'actors': actors,
      'plot': plot,
      'country': country,
      'genre': genre,
      'imdb_rating': imdbRating,
      'imdb_id': imdbId,
    };
  }
}
