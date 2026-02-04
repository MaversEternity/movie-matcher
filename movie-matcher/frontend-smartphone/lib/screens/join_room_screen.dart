import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:uuid/uuid.dart';
import '../providers/connection_provider.dart';
import '../providers/room_provider.dart';
import '../theme/app_theme.dart';

/// Экран присоединения к комнате — по ссылке или QR-коду
class JoinRoomScreen extends ConsumerStatefulWidget {
  const JoinRoomScreen({super.key});

  @override
  ConsumerState<JoinRoomScreen> createState() => _JoinRoomScreenState();
}

class _JoinRoomScreenState extends ConsumerState<JoinRoomScreen> {
  final _controller = TextEditingController();
  bool _isJoining = false;
  bool _showScanner = false;
  String? _error;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Присоединиться'),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_rounded),
          onPressed: () {
            if (_showScanner) {
              setState(() => _showScanner = false);
            } else {
              context.pop();
            }
          },
        ),
      ),
      body: SafeArea(
        child: _showScanner ? _buildScanner() : _buildInputView(),
      ),
    );
  }

  /// Экран ввода ссылки / ID комнаты
  Widget _buildInputView() {
    return Padding(
      padding: const EdgeInsets.all(24),
      child: Column(
        children: [
          const SizedBox(height: 32),

          // Иконка
          Container(
            width: 80,
            height: 80,
            decoration: BoxDecoration(
              color: AppColors.surfaceLight,
              borderRadius: BorderRadius.circular(20),
            ),
            child: const Icon(
              Icons.group_add_rounded,
              size: 40,
              color: AppColors.primary,
            ),
          ),

          const SizedBox(height: 24),

          const Text(
            'Введите ссылку на комнату\nили отсканируйте QR-код',
            style: TextStyle(fontSize: 16, color: AppColors.textSecondary),
            textAlign: TextAlign.center,
          ),

          const SizedBox(height: 32),

          // Поле ввода ссылки
          TextField(
            controller: _controller,
            decoration: const InputDecoration(
              hintText: 'Ссылка или ID комнаты',
              prefixIcon: Icon(Icons.link_rounded, color: AppColors.textHint),
            ),
            style: const TextStyle(color: AppColors.textPrimary),
            onSubmitted: (_) => _joinRoom(),
          ),

          const SizedBox(height: 16),

          // Кнопка присоединиться
          SizedBox(
            width: double.infinity,
            height: 56,
            child: DecoratedBox(
              decoration: BoxDecoration(
                gradient: AppGradients.button,
                borderRadius: BorderRadius.circular(16),
              ),
              child: ElevatedButton(
                onPressed: _isJoining ? null : _joinRoom,
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.transparent,
                  shadowColor: Colors.transparent,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(16),
                  ),
                ),
                child: _isJoining
                    ? const SizedBox(
                        width: 24,
                        height: 24,
                        child: CircularProgressIndicator(
                          strokeWidth: 2,
                          color: Colors.white,
                        ),
                      )
                    : const Text(
                        'Присоединиться',
                        style:
                            TextStyle(fontSize: 17, fontWeight: FontWeight.w600),
                      ),
              ),
            ),
          ),

          const SizedBox(height: 24),

          // Разделитель
          const Row(
            children: [
              Expanded(child: Divider(color: AppColors.divider)),
              Padding(
                padding: EdgeInsets.symmetric(horizontal: 16),
                child: Text(
                  'или',
                  style: TextStyle(color: AppColors.textHint, fontSize: 14),
                ),
              ),
              Expanded(child: Divider(color: AppColors.divider)),
            ],
          ),

          const SizedBox(height: 24),

          // Кнопка сканирования QR
          SizedBox(
            width: double.infinity,
            height: 56,
            child: OutlinedButton.icon(
              onPressed: () => setState(() => _showScanner = true),
              icon: const Icon(Icons.qr_code_scanner_rounded),
              label: const Text(
                'Сканировать QR-код',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
              ),
              style: OutlinedButton.styleFrom(
                side: const BorderSide(color: AppColors.primary, width: 1.5),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(16),
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
      ),
    );
  }

  /// QR-сканер
  Widget _buildScanner() {
    return Stack(
      children: [
        MobileScanner(
          onDetect: (capture) {
            final barcode = capture.barcodes.firstOrNull;
            if (barcode?.rawValue != null) {
              setState(() => _showScanner = false);
              _controller.text = barcode!.rawValue!;
              _joinRoom();
            }
          },
        ),

        // Рамка сканера
        Center(
          child: Container(
            width: 250,
            height: 250,
            decoration: BoxDecoration(
              border: Border.all(color: AppColors.primary, width: 2),
              borderRadius: BorderRadius.circular(20),
            ),
          ),
        ),

        // Подсказка
        const Positioned(
          bottom: 100,
          left: 0,
          right: 0,
          child: Text(
            'Наведите камеру на QR-код',
            style: TextStyle(
              color: Colors.white,
              fontSize: 16,
              fontWeight: FontWeight.w500,
            ),
            textAlign: TextAlign.center,
          ),
        ),
      ],
    );
  }

  /// Извлечь roomId из ссылки или использовать как ID напрямую
  String _extractRoomId(String input) {
    input = input.trim();
    // Если это URL — извлекаем roomId из пути
    final uri = Uri.tryParse(input);
    if (uri != null && uri.pathSegments.isNotEmpty) {
      // Ищем паттерн /room/{roomId} или /api/rooms/{roomId}
      final segments = uri.pathSegments;
      for (int i = 0; i < segments.length; i++) {
        if ((segments[i] == 'room' || segments[i] == 'rooms') &&
            i + 1 < segments.length) {
          return segments[i + 1];
        }
      }
      // Если не нашли паттерн, берём последний сегмент
      return segments.last;
    }
    return input;
  }

  Future<void> _joinRoom() async {
    final input = _controller.text.trim();
    if (input.isEmpty) {
      setState(() => _error = 'Введите ссылку или ID комнаты');
      return;
    }

    setState(() {
      _isJoining = true;
      _error = null;
    });

    try {
      final roomId = _extractRoomId(input);
      final participantId = const Uuid().v4();
      final api = ref.read(apiServiceProvider);

      // Проверяем существование комнаты
      await api.getRoomInfo(roomId);

      // Присоединяемся
      final result = await api.joinRoom(roomId, participantId);

      if (!result.success) {
        setState(() => _error = result.message);
        return;
      }

      // Инициализируем состояние комнаты
      ref.read(roomProvider.notifier).initAsParticipant(
            roomId: roomId,
            participantId: participantId,
            participantsCount: result.room?.participantsCount ?? 1,
          );

      if (mounted) {
        context.go('/lobby');
      }
    } catch (e) {
      setState(() {
        _error = 'Ошибка: комната не найдена или недоступна';
      });
    } finally {
      setState(() => _isJoining = false);
    }
  }
}
