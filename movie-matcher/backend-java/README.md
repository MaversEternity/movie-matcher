# Movie Matcher Backend - Java 25 + Quarkus 3.30.8

Современная реализация backend для приложения Movie Matcher на Java 25 и Quarkus.

## Возможности

- **Java 25**: Использование современных возможностей Java (Records, Sealed Interfaces, Pattern Matching)
- **Quarkus 3.30.8**: Современный реактивный фреймворк с быстрым стартом
- **WebSocket**: Реал-тайм коммуникация для синхронизации участников
- **REST API**: RESTful endpoints для управления комнатами
- **Reactive Programming**: Построено на Mutiny для неблокирующих операций
- **OMDB Integration**: Интеграция с внешним API для получения фильмов

## Требования

- Java 25
- Maven 3.9+

## Быстрый старт

### Сборка проекта

```bash
mvn clean package -DskipTests
```

### Запуск приложения

#### Использование скрипта запуска (рекомендуется)

```bash
cd ..
./start-backend-java.sh
```

#### Запуск через Maven напрямую

```bash
mvn quarkus:dev
```

#### Или с использованием Maven wrapper

```bash
./mvnw quarkus:dev
```

**Примечание**: Рекомендуется использовать системный Maven 3.8.6+ вместо Maven wrapper, так как wrapper может использовать устаревшую версию Maven 3.6.3.

Приложение будет доступно на `http://localhost:3000`

## Конфигурация

### Переменные окружения

Создайте файл `.env` в директории `backend-java`:

```bash
OMDB_API_KEY=your_api_key_here
```

Получите бесплатный API ключ на: https://www.omdbapi.com/apikey.aspx

### Настройки в application.properties

- `OMDB_API_KEY`: API ключ для OMDB (обязательно)
- `QUARKUS_HTTP_PORT`: HTTP порт (по умолчанию: 3000)
- `quarkus.http.cors`: CORS настройки (по умолчанию включены для всех источников)

## API Endpoints

### Health Check
- `GET /health` - Проверка состояния сервиса

### Комнаты
- `POST /api/rooms` - Создать комнату
- `GET /api/rooms/{roomId}` - Получить информацию о комнате
- `GET /api/rooms/{roomId}/state` - Получить состояние комнаты с лайками
- `POST /api/rooms/{roomId}/join` - Присоединиться к комнате
- `PUT /api/rooms/{roomId}/filters` - Обновить фильтры
- `POST /api/rooms/{roomId}/start` - Начать матчинг

### WebSocket
- `WS /ws/rooms/{roomId}` - WebSocket для реал-тайм обновлений

## Примеры использования

### Создание комнаты

```bash
curl -X POST http://localhost:3000/api/rooms \
  -H "Content-Type: application/json" \
  -d '{
    "host_id": "user-123",
    "filters": {
      "genre": "action",
      "year_from": 2020,
      "year_to": 2024
    }
  }'
```

### Присоединение к комнате

```bash
curl -X POST http://localhost:3000/api/rooms/{roomId}/join \
  -H "Content-Type: application/json" \
  -d '{"participant_id": "user-456"}'
```

## Архитектура

### Лучшие практики

- **Layered Architecture**: Resources → Services → Clients
- **Immutable DTOs**: Использование Records для безопасной передачи данных
- **Type-safe Messaging**: Sealed Interfaces для WebSocket сообщений
- **Thread-safe**: ConcurrentHashMap, CopyOnWriteArrayList
- **Reactive**: Неблокирующие операции с Mutiny

### Структура проекта

```
src/main/java/com/moviematcher/
├── client/          # REST клиенты (OMDB)
├── model/           # Модели данных (Records)
├── resource/        # REST endpoints
├── service/         # Бизнес логика
└── websocket/       # WebSocket endpoints
```

## Технологии

- Java 25
- Quarkus 3.30.8
- Mutiny (Reactive)
- Jakarta WebSocket
- REST Client
- Hibernate Validator
- Micrometer (metrics)
- SmallRye Health

## Мониторинг

- Health: `http://localhost:3000/q/health`
- Metrics: `http://localhost:3000/q/metrics`

## Примечания

- Dev mode Quarkus пока не полностью поддерживает Java 25 (ASM ограничение)
- Production JAR работает без проблем с Java 25
- Полная поддержка Java 25 ожидается в Quarkus 4.0 (September 2026)

## Источники

- [Quarkus 3.30.8 Release](https://quarkus.io/blog/quarkus-3-30-8-released/)
- [Java 25 Support Discussion](https://github.com/quarkusio/quarkus/discussions/50130)
- [Quarkus Roadmap](https://github.com/quarkusio/quarkus/discussions/52020)
