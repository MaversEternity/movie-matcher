import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:qr_flutter/qr_flutter.dart';
import 'package:uuid/uuid.dart';
import '../providers/connection_provider.dart';
import '../providers/room_provider.dart';
import '../theme/app_theme.dart';

/// Экран создания комнаты
class CreateRoomScreen extends ConsumerStatefulWidget {
  const CreateRoomScreen({super.key});

  @override
  ConsumerState<CreateRoomScreen> createState() => _CreateRoomScreenState();
}

class _CreateRoomScreenState extends ConsumerState<CreateRoomScreen> {
  bool _isCreating = false;
  String? _error;
  String? _roomId;
  String? _joinUrl;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Создать комнату'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_rounded),
          onPressed: () => context.pop(),
        ),
      ),
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: _roomId != null ? _buildShareView() : _buildCreateView(),
        ),
      ),
    );
  }

  /// Экран до создания комнаты
  Widget _buildCreateView() {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        // Иконка
        Container(
          width: 80,
          height: 80,
          decoration: BoxDecoration(
            color: AppColors.surfaceLight,
            borderRadius: BorderRadius.circular(20),
          ),
          child: const Icon(
            Icons.meeting_room_rounded,
            size: 40,
            color: AppColors.primary,
          ),
        ),

        const SizedBox(height: 24),

        const Text(
          'Создайте комнату и пригласите\nдрузей для выбора фильма',
          style: TextStyle(
            fontSize: 16,
            color: AppColors.textSecondary,
          ),
          textAlign: TextAlign.center,
        ),

        const SizedBox(height: 40),

        // Кнопка создания
        SizedBox(
          width: double.infinity,
          height: 56,
          child: DecoratedBox(
            decoration: BoxDecoration(
              gradient: AppGradients.button,
              borderRadius: BorderRadius.circular(16),
            ),
            child: ElevatedButton(
              onPressed: _isCreating ? null : _createRoom,
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.transparent,
                shadowColor: Colors.transparent,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
                ),
              ),
              child: _isCreating
                  ? const SizedBox(
                      width: 24,
                      height: 24,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Text(
                      'Создать',
                      style: TextStyle(fontSize: 17, fontWeight: FontWeight.w600),
                    ),
            ),
          ),
        ),

        if (_error != null) ...[
          const SizedBox(height: 16),
          Text(
            _error!,
            style: const TextStyle(color: AppColors.dislike, fontSize: 14),
            textAlign: TextAlign.center,
          ),
        ],
      ],
    );
  }

  /// Экран после создания — QR-код и ссылка для приглашения
  Widget _buildShareView() {
    return SingleChildScrollView(
      child: Column(
        children: [
          const Icon(
            Icons.check_circle_rounded,
            size: 48,
            color: AppColors.like,
          ),

          const SizedBox(height: 12),

          const Text(
            'Комната создана!',
            style: TextStyle(
              fontSize: 22,
              fontWeight: FontWeight.bold,
              color: AppColors.textPrimary,
            ),
          ),

          const SizedBox(height: 8),

          const Text(
            'Поделитесь ссылкой или QR-кодом\nс друзьями',
            style: TextStyle(fontSize: 15, color: AppColors.textHint),
            textAlign: TextAlign.center,
          ),

          const SizedBox(height: 32),

          // QR-код
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(20),
            ),
            child: QrImageView(
              data: _joinUrl ?? '',
              version: QrVersions.auto,
              size: 200,
              backgroundColor: Colors.white,
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

          const SizedBox(height: 24),

          // Ссылка
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            decoration: BoxDecoration(
              color: AppColors.surfaceLight,
              borderRadius: BorderRadius.circular(12),
            ),
            child: Row(
              children: [
                Expanded(
                  child: Text(
                    _joinUrl ?? '',
                    style: const TextStyle(
                      fontSize: 13,
                      color: AppColors.textSecondary,
                    ),
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                const SizedBox(width: 8),
                IconButton(
                  icon: const Icon(Icons.copy_rounded, size: 20),
                  color: AppColors.primary,
                  onPressed: () {
                    Clipboard.setData(ClipboardData(text: _joinUrl ?? ''));
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Ссылка скопирована')),
                    );
                  },
                ),
              ],
            ),
          ),

          const SizedBox(height: 32),

          // Кнопка перейти в лобби
          SizedBox(
            width: double.infinity,
            height: 56,
            child: DecoratedBox(
              decoration: BoxDecoration(
                gradient: AppGradients.button,
                borderRadius: BorderRadius.circular(16),
              ),
              child: ElevatedButton(
                onPressed: () => context.go('/lobby'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.transparent,
                  shadowColor: Colors.transparent,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                ),
                child: const Text(
                  'Перейти в комнату',
                  style: TextStyle(fontSize: 17, fontWeight: FontWeight.w600),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _createRoom() async {
    setState(() {
      _isCreating = true;
      _error = null;
    });

    try {
      final hostId = const Uuid().v4();
      final api = ref.read(apiServiceProvider);
      final result = await api.createRoom(hostId);

      // Инициализируем состояние комнаты
      ref.read(roomProvider.notifier).initAsHost(
            roomId: result.roomId,
            participantId: hostId,
            joinUrl: result.joinUrl,
          );

      setState(() {
        _roomId = result.roomId;
        _joinUrl = result.joinUrl;
      });
    } catch (e) {
      setState(() {
        _error = 'Ошибка создания комнаты: ${e.toString()}';
      });
    } finally {
      setState(() => _isCreating = false);
    }
  }
}
