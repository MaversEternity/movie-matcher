import 'package:flutter_test/flutter_test.dart';
import 'package:movie_matcher/models/movie_data.dart';
import 'package:movie_matcher/models/server_message.dart';
import 'package:movie_matcher/models/client_message.dart';

void main() {
  group('MovieData', () {
    test('fromJson парсит корректно', () {
      final json = {
        'title': 'Бойцовский клуб',
        'year': '1999',
        'rated': 'R',
        'runtime': '139 min',
        'poster': 'https://example.com/poster.jpg',
        'director': 'David Fincher',
        'actors': 'Brad Pitt, Edward Norton',
        'plot': 'An insomniac office worker...',
        'country': 'USA',
        'genre': 'Drama',
        'imdb_rating': '8.8',
        'imdb_id': 'tt0137523',
      };

      final movie = MovieData.fromJson(json);
      expect(movie.title, 'Бойцовский клуб');
      expect(movie.year, '1999');
      expect(movie.imdbRating, '8.8');
      expect(movie.imdbId, 'tt0137523');
    });
  });

  group('ServerMessage', () {
    test('парсит ParticipantJoined', () {
      final json = {
        'type': 'ParticipantJoined',
        'participant_id': 'user-123',
      };
      final msg = ServerMessage.fromJson(json);
      expect(msg, isA<ParticipantJoinedMessage>());
      expect((msg as ParticipantJoinedMessage).participantId, 'user-123');
    });

    test('парсит NewMovie', () {
      final json = {
        'type': 'NewMovie',
        'movie': {
          'title': 'Test',
          'year': '2020',
          'imdb_id': 'tt1234567',
        },
      };
      final msg = ServerMessage.fromJson(json);
      expect(msg, isA<NewMovieMessage>());
      expect((msg as NewMovieMessage).movie.title, 'Test');
    });

    test('парсит VotingStarted', () {
      final json = {'type': 'VotingStarted'};
      final msg = ServerMessage.fromJson(json);
      expect(msg, isA<VotingStartedMessage>());
    });
  });

  group('ClientMessage', () {
    test('VoteMessage сериализуется корректно', () {
      final msg = VoteMessage(
        participantId: 'user-1',
        movieId: 'tt0137523',
        isLike: true,
      );
      final json = msg.toJson();
      expect(json['type'], 'Vote');
      expect(json['participant_id'], 'user-1');
      expect(json['movie_id'], 'tt0137523');
      expect(json['is_like'], true);
    });

    test('SetFiltersMessage сериализуется корректно', () {
      final msg = SetFiltersMessage(
        participantId: 'user-1',
        genre: 'Action',
        yearFrom: 2000,
        minRating: 7.0,
      );
      final json = msg.toJson();
      expect(json['type'], 'SetFilters');
      expect(json['genre'], 'Action');
      expect(json['year_from'], 2000);
      expect(json['min_rating'], 7.0);
      expect(json.containsKey('year_to'), false);
    });

    test('ReadyToVoteMessage сериализуется корректно', () {
      final msg = ReadyToVoteMessage(participantId: 'user-1');
      final json = msg.toJson();
      expect(json['type'], 'ReadyToVote');
      expect(json['participant_id'], 'user-1');
    });
  });
}
