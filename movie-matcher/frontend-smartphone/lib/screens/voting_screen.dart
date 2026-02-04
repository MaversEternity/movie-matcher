import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../models/client_message.dart';
import '../models/movie_data.dart';
import '../providers/connection_provider.dart';
import '../providers/room_provider.dart';
import '../providers/voting_provider.dart';
import '../theme/app_theme.dart';

/// Экран голосования — свайп вверх (лайк) / вниз (дизлайк) в стиле Tinder
class VotingScreen extends ConsumerStatefulWidget {
  const VotingScreen({super.key});

  @override
  ConsumerState<VotingScreen> createState() => _VotingScreenState();
}

class _VotingScreenState extends ConsumerState<VotingScreen>
    with TickerProviderStateMixin {
  // Анимация свайпа
  Offset _dragOffset = Offset.zero;
  late AnimationController _swipeController;
  late Animation<Offset> _swipeAnimation;
  bool _isDragging = false;

  @override
  void initState() {
    super.initState();
    _swipeController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 300),
    );
    _swipeAnimation =
        Tween<Offset>(begin: Offset.zero, end: Offset.zero).animate(
      CurvedAnimation(parent: _swipeController, curve: Curves.easeOut),
    );
  }

  @override
  void dispose() {
    _swipeController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final voting = ref.watch(votingProvider);
    final room = ref.watch(roomProvider);

    // Автопереход на результаты
    ref.listen<VotingState>(votingProvider, (prev, next) {
      if (next.isCompleted && next.matchedMovies.isNotEmpty) {
        context.go('/results');
      }
    });

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: const Text('Голосование'),
        automaticallyImplyLeading: false,
        actions: [
          if (voting.currentRound > 0)
            Padding(
              padding: const EdgeInsets.only(right: 16),
              child: Center(
                child: Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  decoration: BoxDecoration(
                    color: AppColors.primary.withOpacity(0.2),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Text(
                    'Раунд ${voting.currentRound}',
                    style: const TextStyle(
                      color: AppColors.primary,
                      fontSize: 13,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
      body: voting.currentMovie != null
          ? _buildSwipeCard(voting.currentMovie!, room)
          : _buildWaitingState(voting),
    );
  }

  /// Карточка фильма с жестами свайпа
  Widget _buildSwipeCard(MovieData movie, RoomState room) {
    final screenSize = MediaQuery.of(context).size;

    // Вычисляем прогресс свайпа для визуальных эффектов
    final swipeProgress = _dragOffset.dy / (screenSize.height * 0.3);
    final isLikeDirection = _dragOffset.dy < 0; // вверх = лайк
    final absProgress = swipeProgress.abs().clamp(0.0, 1.0);

    return GestureDetector(
      onPanStart: (_) {
        setState(() => _isDragging = true);
      },
      onPanUpdate: (details) {
        setState(() {
          _dragOffset += details.delta;
        });
      },
      onPanEnd: (details) {
        _isDragging = false;
        final threshold = screenSize.height * 0.15;

        if (_dragOffset.dy.abs() > threshold) {
          // Свайп зафиксирован
          final isLike = _dragOffset.dy < 0; // вверх = лайк
          _animateSwipeAway(isLike, movie, room);
        } else {
          // Вернуть обратно
          _animateBack();
        }
      },
      child: AnimatedBuilder(
        animation: _swipeController,
        builder: (context, child) {
          final offset = _swipeController.isAnimating
              ? _swipeAnimation.value
              : _dragOffset;

          return Transform.translate(
            offset: offset,
            child: Transform.rotate(
              angle: offset.dx * 0.001, // лёгкий наклон
              child: child,
            ),
          );
        },
        child: Stack(
          children: [
            // Основная карточка
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 100),
              child: _buildMovieCard(movie),
            ),

            // Индикатор лайка (вверху)
            if (_isDragging && isLikeDirection && absProgress > 0.1)
              Positioned(
                top: 80,
                left: 0,
                right: 0,
                child: Center(
                  child: Opacity(
                    opacity: absProgress,
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 24, vertical: 12),
                      decoration: BoxDecoration(
                        color: AppColors.like.withOpacity(0.9),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: const Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.favorite_rounded,
                              color: Colors.white, size: 28),
                          SizedBox(width: 8),
                          Text(
                            'НРАВИТСЯ',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 20,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),

            // Индикатор дизлайка (внизу)
            if (_isDragging && !isLikeDirection && absProgress > 0.1)
              Positioned(
                bottom: 140,
                left: 0,
                right: 0,
                child: Center(
                  child: Opacity(
                    opacity: absProgress,
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 24, vertical: 12),
                      decoration: BoxDecoration(
                        color: AppColors.dislike.withOpacity(0.9),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: const Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(Icons.close_rounded,
                              color: Colors.white, size: 28),
                          SizedBox(width: 8),
                          Text(
                            'НЕ НРАВИТСЯ',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 20,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ),
              ),

            // Кнопки лайк/дизлайк внизу
            Positioned(
              bottom: 24,
              left: 0,
              right: 0,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  // Дизлайк
                  _buildActionButton(
                    icon: Icons.close_rounded,
                    color: AppColors.dislike,
                    onTap: () =>
                        _animateSwipeAway(false, movie, ref.read(roomProvider)),
                  ),
                  const SizedBox(width: 40),
                  // Лайк
                  _buildActionButton(
                    icon: Icons.favorite_rounded,
                    color: AppColors.like,
                    onTap: () =>
                        _animateSwipeAway(true, movie, ref.read(roomProvider)),
                  ),
                ],
              ),
            ),

            // Подсказка о свайпах
            const Positioned(
              bottom: 80,
              left: 0,
              right: 0,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Icon(Icons.arrow_upward_rounded,
                      size: 14, color: AppColors.textMuted),
                  SizedBox(width: 4),
                  Text(
                    'свайп вверх — нравится  |  свайп вниз — не нравится',
                    style: TextStyle(color: AppColors.textMuted, fontSize: 11),
                  ),
                  SizedBox(width: 4),
                  Icon(Icons.arrow_downward_rounded,
                      size: 14, color: AppColors.textMuted),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Карточка с информацией о фильме
  Widget _buildMovieCard(MovieData movie) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.3),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(20),
        child: Stack(
          fit: StackFit.expand,
          children: [
            // Постер на весь экран
            if (movie.poster != null && movie.poster!.isNotEmpty)
              CachedNetworkImage(
                imageUrl: movie.poster!,
                fit: BoxFit.cover,
                placeholder: (_, __) => Container(
                  color: AppColors.surfaceCard,
                  child: const Center(
                    child: Icon(Icons.movie_rounded,
                        size: 60, color: AppColors.textHint),
                  ),
                ),
                errorWidget: (_, __, ___) => Container(
                  color: AppColors.surfaceCard,
                  child: const Center(
                    child: Icon(Icons.movie_rounded,
                        size: 60, color: AppColors.textHint),
                  ),
                ),
              )
            else
              Container(
                color: AppColors.surfaceCard,
                child: const Center(
                  child: Icon(Icons.movie_rounded,
                      size: 60, color: AppColors.textHint),
                ),
              ),

            // Градиент для текста
            Container(
              decoration: BoxDecoration(
                gradient: LinearGradient(
                  begin: Alignment.topCenter,
                  end: Alignment.bottomCenter,
                  colors: [
                    Colors.transparent,
                    Colors.transparent,
                    Colors.black.withOpacity(0.7),
                    Colors.black.withOpacity(0.95),
                  ],
                  stops: const [0.0, 0.4, 0.7, 1.0],
                ),
              ),
            ),

            // Информация о фильме
            Positioned(
              bottom: 0,
              left: 0,
              right: 0,
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    // Название
                    Text(
                      movie.title ?? 'Без названия',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 24,
                        fontWeight: FontWeight.bold,
                      ),
                    ),

                    const SizedBox(height: 8),

                    // Год, рейтинг, жанр
                    Row(
                      children: [
                        if (movie.year != null) ...[
                          _buildTag(movie.year!),
                          const SizedBox(width: 8),
                        ],
                        if (movie.imdbRating != null &&
                            movie.imdbRating!.isNotEmpty) ...[
                          _buildTag(
                            '${movie.imdbRating}',
                            icon: Icons.star_rounded,
                            color: AppColors.warning,
                          ),
                          const SizedBox(width: 8),
                        ],
                        if (movie.runtime != null &&
                            movie.runtime!.isNotEmpty)
                          _buildTag(movie.runtime!),
                      ],
                    ),

                    const SizedBox(height: 8),

                    // Жанр
                    if (movie.genre != null && movie.genre!.isNotEmpty)
                      Text(
                        movie.genre!,
                        style: const TextStyle(
                          color: AppColors.textSecondary,
                          fontSize: 14,
                        ),
                      ),

                    const SizedBox(height: 8),

                    // Описание (краткое)
                    if (movie.plot != null && movie.plot!.isNotEmpty)
                      Text(
                        movie.plot!,
                        maxLines: 3,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          color: Colors.white.withOpacity(0.8),
                          fontSize: 13,
                          height: 1.4,
                        ),
                      ),

                    const SizedBox(height: 6),

                    // Режиссёр, актёры
                    if (movie.director != null && movie.director!.isNotEmpty)
                      Text(
                        'Режиссёр: ${movie.director}',
                        style: const TextStyle(
                          color: AppColors.textHint,
                          fontSize: 12,
                        ),
                      ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Тег (год, рейтинг и т.д.)
  Widget _buildTag(String text, {IconData? icon, Color? color}) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.white.withOpacity(0.15),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            Icon(icon, size: 14, color: color ?? Colors.white),
            const SizedBox(width: 4),
          ],
          Text(
            text,
            style: TextStyle(
              color: color ?? Colors.white,
              fontSize: 13,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  /// Круглая кнопка действия (лайк / дизлайк)
  Widget _buildActionButton({
    required IconData icon,
    required Color color,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Container(
        width: 64,
        height: 64,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          color: color.withOpacity(0.15),
          border: Border.all(color: color, width: 2),
        ),
        child: Icon(icon, color: color, size: 32),
      ),
    );
  }

  /// Экран ожидания (нет фильмов в очереди)
  Widget _buildWaitingState(VotingState voting) {
    return Center(
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          if (voting.noMoreMovies) ...[
            const Icon(Icons.movie_filter_outlined,
                size: 64, color: AppColors.textHint),
            const SizedBox(height: 16),
            const Text(
              'Фильмы закончились',
              style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w600,
                  color: AppColors.textPrimary),
            ),
            const SizedBox(height: 8),
            const Text(
              'Ожидание результатов голосования...',
              style: TextStyle(color: AppColors.textHint),
            ),
          ] else ...[
            const SizedBox(
              width: 48,
              height: 48,
              child: CircularProgressIndicator(
                strokeWidth: 3,
                color: AppColors.primary,
              ),
            ),
            const SizedBox(height: 20),
            const Text(
              'Загрузка фильмов...',
              style: TextStyle(
                  fontSize: 16,
                  color: AppColors.textSecondary),
            ),
          ],
        ],
      ),
    );
  }

  /// Анимация вылета карточки
  void _animateSwipeAway(bool isLike, MovieData movie, RoomState room) {
    final screenSize = MediaQuery.of(context).size;
    final endOffset = Offset(
      0,
      isLike ? -screenSize.height : screenSize.height,
    );

    _swipeAnimation = Tween<Offset>(
      begin: _dragOffset,
      end: endOffset,
    ).animate(CurvedAnimation(
      parent: _swipeController,
      curve: Curves.easeInOut,
    ));

    _swipeController.forward(from: 0).then((_) {
      // Отправляем голос
      final ws = ref.read(wsServiceProvider);
      ws.send(VoteMessage(
        participantId: room.participantId!,
        movieId: movie.imdbId ?? '',
        isLike: isLike,
      ));

      // Убираем фильм из очереди
      ref.read(votingProvider.notifier).removeCurrentMovie();

      // Сброс состояния
      setState(() {
        _dragOffset = Offset.zero;
      });
      _swipeController.reset();
    });
  }

  /// Вернуть карточку обратно
  void _animateBack() {
    _swipeAnimation = Tween<Offset>(
      begin: _dragOffset,
      end: Offset.zero,
    ).animate(CurvedAnimation(
      parent: _swipeController,
      curve: Curves.easeOut,
    ));

    _swipeController.forward(from: 0).then((_) {
      setState(() {
        _dragOffset = Offset.zero;
      });
      _swipeController.reset();
    });
  }
}
