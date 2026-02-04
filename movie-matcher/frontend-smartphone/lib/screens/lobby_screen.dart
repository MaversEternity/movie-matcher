import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:cached_network_image/cached_network_image.dart';
import '../models/client_message.dart';
import '../models/room_info.dart';
import '../providers/connection_provider.dart';
import '../providers/room_provider.dart';
import '../theme/app_theme.dart';

/// Экран лобби — настройка фильтров, поиск фильмов, ожидание готовности
class LobbyScreen extends ConsumerStatefulWidget {
  const LobbyScreen({super.key});

  @override
  ConsumerState<LobbyScreen> createState() => _LobbyScreenState();
}

class _LobbyScreenState extends ConsumerState<LobbyScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;
  final _searchController = TextEditingController();

  // Фильтры
  String? _selectedGenre;
  int? _yearFrom;
  int? _yearTo;
  double _minRating = 0;
  String _type = 'movie';
  bool _noFilter = false;

  static const _genres = [
    'Action',
    'Comedy',
    'Drama',
    'Horror',
    'Romance',
    'Sci-Fi',
    'Thriller',
    'Adventure',
    'Animation',
    'Fantasy',
    'Mystery',
    'Crime',
  ];

  static const _genreLabels = {
    'Action': 'Боевик',
    'Comedy': 'Комедия',
    'Drama': 'Драма',
    'Horror': 'Ужасы',
    'Romance': 'Романтика',
    'Sci-Fi': 'Научная фантастика',
    'Thriller': 'Триллер',
    'Adventure': 'Приключения',
    'Animation': 'Анимация',
    'Fantasy': 'Фэнтези',
    'Mystery': 'Детектив',
    'Crime': 'Криминал',
  };

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final room = ref.watch(roomProvider);

    // Автопереход на экран голосования
    ref.listen<RoomState>(roomProvider, (prev, next) {
      if (next.status == RoomStatus.voting) {
        context.go('/voting');
      }
    });

    return Scaffold(
      appBar: AppBar(
        title: Text('Комната ${room.roomId?.substring(0, 6) ?? ''}'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_rounded),
          onPressed: () => _showLeaveDialog(),
        ),
        actions: [
          if (room.isHost)
            IconButton(
              icon: const Icon(Icons.share_rounded),
              onPressed: () => _showShareSheet(),
            ),
        ],
      ),
      body: Column(
        children: [
          // Индикатор участников и готовности
          _buildParticipantsBar(room),

          // Табы: Фильтры / Поиск
          Container(
            color: AppColors.surface,
            child: TabBar(
              controller: _tabController,
              indicatorColor: AppColors.primary,
              labelColor: AppColors.primary,
              unselectedLabelColor: AppColors.textHint,
              tabs: const [
                Tab(text: 'Фильтры', icon: Icon(Icons.tune_rounded, size: 20)),
                Tab(
                    text: 'Поиск',
                    icon: Icon(Icons.search_rounded, size: 20)),
              ],
            ),
          ),

          // Контент табов
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                _buildFiltersTab(),
                _buildSearchTab(),
              ],
            ),
          ),

          // Кнопка «Готов к голосованию»
          _buildReadyButton(room),
        ],
      ),
    );
  }

  /// Полоска с количеством участников и готовностью
  Widget _buildParticipantsBar(RoomState room) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
      color: AppColors.surfaceLight,
      child: Row(
        children: [
          const Icon(Icons.people_rounded, size: 20, color: AppColors.primary),
          const SizedBox(width: 8),
          Text(
            'Участников: ${room.participantsCount}',
            style: const TextStyle(
              color: AppColors.textSecondary,
              fontSize: 14,
            ),
          ),
          const Spacer(),
          if (room.readyCount > 0) ...[
            const Icon(Icons.check_circle_outline,
                size: 18, color: AppColors.like),
            const SizedBox(width: 6),
            Text(
              'Готовы: ${room.readyCount}/${room.totalCount}',
              style: const TextStyle(
                color: AppColors.like,
                fontSize: 14,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ],
      ),
    );
  }

  /// Вкладка «Фильтры»
  Widget _buildFiltersTab() {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Опция «Без фильтра»
          SwitchListTile(
            title: const Text(
              'Без фильтра (рандомные фильмы)',
              style: TextStyle(color: AppColors.textPrimary, fontSize: 15),
            ),
            value: _noFilter,
            activeColor: AppColors.primary,
            contentPadding: EdgeInsets.zero,
            onChanged: (val) => setState(() => _noFilter = val),
          ),

          if (!_noFilter) ...[
            const SizedBox(height: 16),

            // Жанр
            const Text('Жанр',
                style: TextStyle(
                    color: AppColors.textHint,
                    fontSize: 13,
                    fontWeight: FontWeight.w500)),
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              decoration: BoxDecoration(
                color: AppColors.surfaceLight,
                borderRadius: BorderRadius.circular(12),
              ),
              child: DropdownButtonHideUnderline(
                child: DropdownButton<String>(
                  value: _selectedGenre,
                  isExpanded: true,
                  hint: const Text('Выберите жанр',
                      style: TextStyle(color: AppColors.textHint)),
                  dropdownColor: AppColors.surfaceCard,
                  items: _genres
                      .map((g) => DropdownMenuItem(
                            value: g,
                            child: Text(
                              _genreLabels[g] ?? g,
                              style: const TextStyle(
                                  color: AppColors.textPrimary),
                            ),
                          ))
                      .toList(),
                  onChanged: (val) => setState(() => _selectedGenre = val),
                ),
              ),
            ),

            const SizedBox(height: 20),

            // Год
            const Text('Год выпуска',
                style: TextStyle(
                    color: AppColors.textHint,
                    fontSize: 13,
                    fontWeight: FontWeight.w500)),
            const SizedBox(height: 8),
            Row(
              children: [
                Expanded(
                  child: TextField(
                    decoration: const InputDecoration(hintText: 'От'),
                    keyboardType: TextInputType.number,
                    inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                    style: const TextStyle(color: AppColors.textPrimary),
                    onChanged: (val) =>
                        _yearFrom = int.tryParse(val),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: TextField(
                    decoration: InputDecoration(
                        hintText: 'До (${DateTime.now().year})'),
                    keyboardType: TextInputType.number,
                    inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                    style: const TextStyle(color: AppColors.textPrimary),
                    onChanged: (val) =>
                        _yearTo = int.tryParse(val),
                  ),
                ),
              ],
            ),

            const SizedBox(height: 20),

            // Рейтинг
            Text(
              'Минимальный рейтинг: ${_minRating.toStringAsFixed(1)}',
              style: const TextStyle(
                  color: AppColors.textHint,
                  fontSize: 13,
                  fontWeight: FontWeight.w500),
            ),
            Slider(
              value: _minRating,
              min: 0,
              max: 10,
              divisions: 20,
              activeColor: AppColors.primary,
              inactiveColor: AppColors.surfaceLight,
              label: _minRating.toStringAsFixed(1),
              onChanged: (val) => setState(() => _minRating = val),
            ),

            const SizedBox(height: 12),

            // Тип: фильм / сериал
            const Text('Тип',
                style: TextStyle(
                    color: AppColors.textHint,
                    fontSize: 13,
                    fontWeight: FontWeight.w500)),
            const SizedBox(height: 8),
            Row(
              children: [
                _buildTypeChip('movie', 'Фильм'),
                const SizedBox(width: 10),
                _buildTypeChip('series', 'Сериал'),
              ],
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildTypeChip(String value, String label) {
    final selected = _type == value;
    return GestureDetector(
      onTap: () => setState(() => _type = value),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
        decoration: BoxDecoration(
          color: selected
              ? AppColors.primary.withOpacity(0.2)
              : AppColors.surfaceLight,
          borderRadius: BorderRadius.circular(20),
          border: selected
              ? Border.all(color: AppColors.primary, width: 1.5)
              : null,
        ),
        child: Text(
          label,
          style: TextStyle(
            color: selected ? AppColors.primary : AppColors.textSecondary,
            fontWeight: selected ? FontWeight.w600 : FontWeight.normal,
          ),
        ),
      ),
    );
  }

  /// Вкладка «Поиск»
  Widget _buildSearchTab() {
    final room = ref.watch(roomProvider);

    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        children: [
          // Поле поиска
          TextField(
            controller: _searchController,
            decoration: InputDecoration(
              hintText: 'Название фильма...',
              prefixIcon:
                  const Icon(Icons.search_rounded, color: AppColors.textHint),
              suffixIcon: IconButton(
                icon: const Icon(Icons.send_rounded, color: AppColors.primary),
                onPressed: _searchMovie,
              ),
            ),
            style: const TextStyle(color: AppColors.textPrimary),
            onSubmitted: (_) => _searchMovie(),
          ),

          const SizedBox(height: 16),

          // Результаты поиска
          Expanded(
            child: room.searchResults.isEmpty
                ? const Center(
                    child: Text(
                      'Найдите фильм и добавьте\nего в свою выборку',
                      style: TextStyle(color: AppColors.textHint, fontSize: 15),
                      textAlign: TextAlign.center,
                    ),
                  )
                : ListView.builder(
                    itemCount: room.searchResults.length,
                    itemBuilder: (context, index) {
                      final movie = room.searchResults[index];
                      final isSelected = room.selectedMovieIds
                          .contains(movie.imdbId);

                      return Container(
                        margin: const EdgeInsets.only(bottom: 10),
                        decoration: BoxDecoration(
                          color: AppColors.surfaceCard,
                          borderRadius: BorderRadius.circular(12),
                          border: isSelected
                              ? Border.all(color: AppColors.like, width: 1.5)
                              : null,
                        ),
                        child: ListTile(
                          contentPadding: const EdgeInsets.all(8),
                          leading: ClipRRect(
                            borderRadius: BorderRadius.circular(8),
                            child: movie.poster != null &&
                                    movie.poster!.isNotEmpty
                                ? CachedNetworkImage(
                                    imageUrl: movie.poster!,
                                    width: 50,
                                    height: 70,
                                    fit: BoxFit.cover,
                                    placeholder: (_, __) => Container(
                                      width: 50,
                                      height: 70,
                                      color: AppColors.surfaceLight,
                                      child: const Icon(Icons.movie_rounded,
                                          color: AppColors.textHint),
                                    ),
                                    errorWidget: (_, __, ___) => Container(
                                      width: 50,
                                      height: 70,
                                      color: AppColors.surfaceLight,
                                      child: const Icon(Icons.movie_rounded,
                                          color: AppColors.textHint),
                                    ),
                                  )
                                : Container(
                                    width: 50,
                                    height: 70,
                                    color: AppColors.surfaceLight,
                                    child: const Icon(Icons.movie_rounded,
                                        color: AppColors.textHint),
                                  ),
                          ),
                          title: Text(
                            movie.title ?? 'Без названия',
                            style: const TextStyle(
                              color: AppColors.textPrimary,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                          subtitle: Text(
                            '${movie.year ?? ''} ${movie.genre ?? ''}',
                            style: const TextStyle(
                              color: AppColors.textHint,
                              fontSize: 13,
                            ),
                          ),
                          trailing: isSelected
                              ? const Icon(Icons.check_circle_rounded,
                                  color: AppColors.like)
                              : IconButton(
                                  icon: const Icon(
                                      Icons.add_circle_outline_rounded,
                                      color: AppColors.primary),
                                  onPressed: () =>
                                      _addMovieToSelection(movie.imdbId!),
                                ),
                        ),
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }

  /// Кнопка «Готов к голосованию»
  Widget _buildReadyButton(RoomState room) {
    return Container(
      padding: const EdgeInsets.fromLTRB(20, 12, 20, 24),
      decoration: const BoxDecoration(
        color: AppColors.surface,
        border: Border(top: BorderSide(color: AppColors.divider, width: 0.5)),
      ),
      child: SizedBox(
        width: double.infinity,
        height: 56,
        child: DecoratedBox(
          decoration: BoxDecoration(
            gradient: room.isReady ? null : AppGradients.button,
            color: room.isReady ? AppColors.like.withOpacity(0.2) : null,
            borderRadius: BorderRadius.circular(16),
          ),
          child: ElevatedButton(
            onPressed: room.isReady ? null : _markReady,
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.transparent,
              shadowColor: Colors.transparent,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
              ),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(
                  room.isReady
                      ? Icons.check_circle_rounded
                      : Icons.how_to_vote_rounded,
                  size: 22,
                ),
                const SizedBox(width: 10),
                Text(
                  room.isReady
                      ? 'Ожидание остальных...'
                      : 'Готов к голосованию',
                  style: const TextStyle(
                      fontSize: 17, fontWeight: FontWeight.w600),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  /// Отправить фильтры и отметить готовность
  void _markReady() {
    final room = ref.read(roomProvider);
    final ws = ref.read(wsServiceProvider);

    // Отправляем фильтры
    if (!_noFilter) {
      ws.send(SetFiltersMessage(
        participantId: room.participantId!,
        genre: _selectedGenre,
        yearFrom: _yearFrom,
        yearTo: _yearTo,
        minRating: _minRating > 0 ? _minRating : null,
        type: _type,
      ));
    }

    // Отправляем готовность
    ws.send(ReadyToVoteMessage(participantId: room.participantId!));
    ref.read(roomProvider.notifier).markReady();
  }

  /// Поиск фильма
  void _searchMovie() {
    final query = _searchController.text.trim();
    if (query.isEmpty) return;

    final room = ref.read(roomProvider);
    final ws = ref.read(wsServiceProvider);

    ref.read(roomProvider.notifier).clearSearchResults();
    ws.send(SearchMovieMessage(
      participantId: room.participantId!,
      query: query,
    ));
  }

  /// Добавить фильм в выборку
  void _addMovieToSelection(String movieId) {
    final room = ref.read(roomProvider);
    final ws = ref.read(wsServiceProvider);

    ws.send(AddMovieToSelectionMessage(
      participantId: room.participantId!,
      movieId: movieId,
    ));
    ref.read(roomProvider.notifier).addSelectedMovie(movieId);

    ScaffoldMessenger.of(context).showSnackBar(
      const SnackBar(content: Text('Фильм добавлен в выборку')),
    );
  }

  /// Диалог «Поделиться комнатой»
  void _showShareSheet() {
    final room = ref.read(roomProvider);
    showModalBottomSheet(
      context: context,
      builder: (ctx) => Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Ручка
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.textHint.withOpacity(0.3),
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: 20),
            const Text(
              'Пригласить друзей',
              style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                  color: AppColors.textPrimary),
            ),
            const SizedBox(height: 20),

            // QR-код
            if (room.joinUrl != null)
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: QrImageView(
                  data: room.joinUrl!,
                  version: QrVersions.auto,
                  size: 180,
                  eyeStyle: const QrEyeStyle(
                    eyeShape: QrEyeShape.roundedOuter,
                    color: Color(0xFF1A1A2E),
                  ),
                  dataModuleStyle: const QrDataModuleStyle(
                    dataModuleShape: QrDataModuleShape.roundedOuter,
                    color: Color(0xFF1A1A2E),
                  ),
                ),
              ),

            const SizedBox(height: 16),

            // Кнопка копирования ссылки
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: () {
                  Clipboard.setData(
                      ClipboardData(text: room.joinUrl ?? ''));
                  Navigator.pop(ctx);
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(content: Text('Ссылка скопирована')),
                  );
                },
                icon: const Icon(Icons.copy_rounded),
                label: const Text('Скопировать ссылку'),
              ),
            ),
            const SizedBox(height: 16),
          ],
        ),
      ),
    );
  }

  /// Диалог подтверждения выхода
  void _showLeaveDialog() {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Покинуть комнату?'),
        content: const Text('Вы уверены, что хотите выйти?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Отмена'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(ctx);
              final room = ref.read(roomProvider);
              final ws = ref.read(wsServiceProvider);
              ws.send(LeaveRoomMessage(participantId: room.participantId!));
              ref.read(roomProvider.notifier).reset();
              context.go('/');
            },
            child: const Text('Выйти',
                style: TextStyle(color: AppColors.dislike)),
          ),
        ],
      ),
    );
  }
}
