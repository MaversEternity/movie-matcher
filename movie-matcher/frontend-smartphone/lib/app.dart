import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'screens/home_screen.dart';
import 'screens/create_room_screen.dart';
import 'screens/join_room_screen.dart';
import 'screens/lobby_screen.dart';
import 'screens/voting_screen.dart';
import 'screens/results_screen.dart';
import 'theme/app_theme.dart';

/// Роутер приложения
final GoRouter _router = GoRouter(
  initialLocation: '/',
  routes: [
    GoRoute(
      path: '/',
      builder: (context, state) => const HomeScreen(),
    ),
    GoRoute(
      path: '/create',
      builder: (context, state) => const CreateRoomScreen(),
    ),
    GoRoute(
      path: '/join',
      builder: (context, state) => const JoinRoomScreen(),
    ),
    // Deep link: /room/{roomId} — перенаправляет на Join с параметром
    GoRoute(
      path: '/room/:roomId',
      redirect: (context, state) {
        final roomId = state.pathParameters['roomId'];
        return '/join?roomId=$roomId';
      },
    ),
    GoRoute(
      path: '/lobby',
      builder: (context, state) => const LobbyScreen(),
    ),
    GoRoute(
      path: '/voting',
      builder: (context, state) => const VotingScreen(),
    ),
    GoRoute(
      path: '/results',
      builder: (context, state) => const ResultsScreen(),
    ),
  ],
);

/// Корневой виджет приложения
class MovieMatcherApp extends StatelessWidget {
  const MovieMatcherApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'Movie Matcher',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.dark,
      routerConfig: _router,
    );
  }
}
