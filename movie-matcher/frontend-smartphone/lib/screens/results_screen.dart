import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../models/movie_data.dart';
import '../providers/room_provider.dart';
import '../providers/voting_provider.dart';
import '../theme/app_theme.dart';

/// Экран результатов — показывает совпавшие фильмы
class ResultsScreen extends ConsumerWidget {
  const ResultsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final voting = ref.watch(votingProvider);
    final matchedMovies = voting.matchedMovies;

    return Scaffold(
      body: SafeArea(
        child: matchedMovies.isEmpty
            ? _buildNoMatch(context, ref)
            : matchedMovies.length == 1
                ? _buildSingleResult(context, ref, matchedMovies.first)
                : _buildMultipleResults(context, ref, matchedMovies),
      ),
    );
  }

  /// Нет совпадений
  Widget _buildNoMatch(BuildContext context, WidgetRef ref) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.sentiment_neutral_rounded,
                size: 80, color: AppColors.textHint),
            const SizedBox(height: 24),
            const Text(
              'Совпадений не найдено',
              style: TextStyle(
                fontSize: 22,
                fontWeight: FontWeight.bold,
                color: AppColors.textPrimary,
              ),
            ),
            const SizedBox(height: 12),
            const Text(
              'К сожалению, ни один фильм\nне понравился всем участникам',
              style: TextStyle(
                fontSize: 15,
                color: AppColors.textHint,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 40),
            _buildHomeButton(context, ref),
          ],
        ),
      ),
    );
  }

  /// Один совпавший фильм — большая карточка
  Widget _buildSingleResult(
      BuildContext context, WidgetRef ref, MovieData movie) {
    return SingleChildScrollView(
      child: Column(
        children: [
          const SizedBox(height: 24),

          // Заголовок
          const Icon(Icons.celebration_rounded,
              size: 48, color: AppColors.warning),
          const SizedBox(height: 12),
          const Text(
            'Совпадение!',
            style: TextStyle(
              fontSize: 26,
              fontWeight: FontWeight.bold,
              color: AppColors.textPrimary,
            ),
          ),
          const SizedBox(height: 4),
          const Text(
            'Все выбрали этот фильм',
            style: TextStyle(fontSize: 15, color: AppColors.textHint),
          ),

          const SizedBox(height: 24),

          // Карточка фильма
          _buildMovieDetailCard(movie),

          const SizedBox(height: 24),

          // Кнопка на главную
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 32),
            child: _buildHomeButton(context, ref),
          ),

          const SizedBox(height: 32),
        ],
      ),
    );
  }

  /// Несколько совпавших фильмов — список
  Widget _buildMultipleResults(
      BuildContext context, WidgetRef ref, List<MovieData> movies) {
    return Column(
      children: [
        const SizedBox(height: 24),

        const Icon(Icons.celebration_rounded,
            size: 48, color: AppColors.warning),
        const SizedBox(height: 12),
        const Text(
          'Совпадения!',
          style: TextStyle(
            fontSize: 26,
            fontWeight: FontWeight.bold,
            color: AppColors.textPrimary,
          ),
        ),
        const SizedBox(height: 4),
        Text(
          'Найдено ${movies.length} фильмов',
          style: const TextStyle(fontSize: 15, color: AppColors.textHint),
        ),

        const SizedBox(height: 20),

        // Список фильмов
        Expanded(
          child: ListView.builder(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            itemCount: movies.length,
            itemBuilder: (context, index) =>
                _buildMovieDetailCard(movies[index]),
          ),
        ),

        // Кнопка на главную
        Padding(
          padding: const EdgeInsets.fromLTRB(32, 12, 32, 24),
          child: _buildHomeButton(context, ref),
        ),
      ],
    );
  }

  /// Детальная карточка фильма
  Widget _buildMovieDetailCard(MovieData movie) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: AppColors.primary.withOpacity(0.15),
            blurRadius: 20,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Постер
          ClipRRect(
            borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
            child: AspectRatio(
              aspectRatio: 2 / 3,
              child: movie.poster != null && movie.poster!.isNotEmpty
                  ? CachedNetworkImage(
                      imageUrl: movie.poster!,
                      fit: BoxFit.cover,
                      placeholder: (_, __) => Container(
                        color: AppColors.surfaceCard,
                        child: const Center(
                          child: Icon(Icons.movie_rounded,
                              size: 60, color: AppColors.textHint),
                        ),
                      ),
                    )
                  : Container(
                      color: AppColors.surfaceCard,
                      child: const Center(
                        child: Icon(Icons.movie_rounded,
                            size: 60, color: AppColors.textHint),
                      ),
                    ),
            ),
          ),

          // Информация
          Padding(
            padding: const EdgeInsets.all(20),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Название
                Text(
                  movie.title ?? 'Без названия',
                  style: const TextStyle(
                    fontSize: 22,
                    fontWeight: FontWeight.bold,
                    color: AppColors.textPrimary,
                  ),
                ),

                const SizedBox(height: 12),

                // Теги
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    if (movie.year != null)
                      _buildInfoChip(Icons.calendar_today_rounded, movie.year!),
                    if (movie.imdbRating != null &&
                        movie.imdbRating!.isNotEmpty)
                      _buildInfoChip(
                        Icons.star_rounded,
                        'IMDb ${movie.imdbRating}',
                        color: AppColors.warning,
                      ),
                    if (movie.runtime != null && movie.runtime!.isNotEmpty)
                      _buildInfoChip(Icons.access_time_rounded, movie.runtime!),
                    if (movie.country != null && movie.country!.isNotEmpty)
                      _buildInfoChip(Icons.public_rounded, movie.country!),
                  ],
                ),

                const SizedBox(height: 12),

                // Жанр
                if (movie.genre != null && movie.genre!.isNotEmpty) ...[
                  Text(
                    movie.genre!,
                    style: const TextStyle(
                      color: AppColors.primary,
                      fontSize: 14,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(height: 12),
                ],

                // Описание
                if (movie.plot != null && movie.plot!.isNotEmpty) ...[
                  Text(
                    movie.plot!,
                    style: const TextStyle(
                      color: AppColors.textSecondary,
                      fontSize: 14,
                      height: 1.5,
                    ),
                  ),
                  const SizedBox(height: 12),
                ],

                // Режиссёр
                if (movie.director != null && movie.director!.isNotEmpty)
                  _buildInfoRow('Режиссёр', movie.director!),

                // Актёры
                if (movie.actors != null && movie.actors!.isNotEmpty)
                  _buildInfoRow('Актёры', movie.actors!),

                // Возрастной рейтинг
                if (movie.rated != null && movie.rated!.isNotEmpty)
                  _buildInfoRow('Рейтинг', movie.rated!),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInfoChip(IconData icon, String text, {Color? color}) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: AppColors.surfaceLight,
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: color ?? AppColors.textHint),
          const SizedBox(width: 4),
          Text(
            text,
            style: TextStyle(
              color: color ?? AppColors.textSecondary,
              fontSize: 13,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 80,
            child: Text(
              label,
              style: const TextStyle(
                color: AppColors.textHint,
                fontSize: 13,
              ),
            ),
          ),
          Expanded(
            child: Text(
              value,
              style: const TextStyle(
                color: AppColors.textSecondary,
                fontSize: 13,
              ),
            ),
          ),
        ],
      ),
    );
  }

  /// Кнопка «На главную»
  Widget _buildHomeButton(BuildContext context, WidgetRef ref) {
    return SizedBox(
      width: double.infinity,
      height: 56,
      child: DecoratedBox(
        decoration: BoxDecoration(
          gradient: AppGradients.button,
          borderRadius: BorderRadius.circular(16),
        ),
        child: ElevatedButton(
          onPressed: () {
            ref.read(roomProvider.notifier).reset();
            ref.read(votingProvider.notifier).reset();
            context.go('/');
          },
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.transparent,
            shadowColor: Colors.transparent,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(16),
            ),
          ),
          child: const Text(
            'На главную',
            style: TextStyle(fontSize: 17, fontWeight: FontWeight.w600),
          ),
        ),
      ),
    );
  }
}
