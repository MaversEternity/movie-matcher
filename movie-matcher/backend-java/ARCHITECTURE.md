# Movie Matcher - Архитектура Backend (ООП Best Practices)

## Обзор

Backend приложение для совместного выбора фильмов с применением лучших практик ООП и паттернов проектирования.

## Ключевые архитектурные решения

### 1. In-Memory хранилище комнат (Heap)
- **Комнаты НЕ персистентны** - живут только в памяти приложения
- Используется `ConcurrentHashMap<String, Room>`
- После выхода всех участников комната удаляется
- Преимущества: быстродействие, простота, нет overhead БД

### 2. БД только для фильмов и сериалов
- Хранение полной информации о фильмах/сериалах
- **Только на русском языке** (первый этап)
- Обогащение из внешних источников (TMDB, OMDB)

## Применяемые Design Patterns

### 1. Adapter Pattern - Унификация источников данных

```java
/**
 * Единый интерфейс для работы с разными источниками данных о фильмах
 */
public interface MovieDataSource {
    List<Movie> findByFilters(MovieFilters filters, int page, int pageSize);
    Optional<Movie> findByExternalId(String externalId);
}

// Адаптеры для разных источников
public class DatabaseMovieDataSource implements MovieDataSource { }
public class TmdbApiDataSource implements MovieDataSource { }
public class OmdbApiDataSource implements MovieDataSource { }
```

**Принципы:**
- **Open/Closed**: новые источники добавляются без изменения существующего кода
- **Dependency Inversion**: зависимость от абстракции `MovieDataSource`
- **Single Responsibility**: каждый адаптер отвечает только за свой источник

### 2. Chain of Responsibility - Поиск фильмов

```java
/**
 * Цепочка поиска фильма: БД → TMDB → OMDB
 * При нахождении через API - сохранение в БД
 */
public abstract class MovieSearchHandler {
    protected MovieSearchHandler next;
    
    public void setNext(MovieSearchHandler handler) {
        this.next = handler;
    }
    
    public abstract Optional<Movie> search(String query);
    
    protected Optional<Movie> searchNext(String query) {
        return next != null ? next.search(query) : Optional.empty();
    }
}

public class DatabaseSearchHandler extends MovieSearchHandler {
    @Override
    public Optional<Movie> search(String query) {
        Optional<Movie> result = database.findByTitle(query);
        return result.isPresent() ? result : searchNext(query);
    }
}

public class TmdbSearchHandler extends MovieSearchHandler {
    @Override
    public Optional<Movie> search(String query) {
        Optional<Movie> result = tmdbApi.search(query);
        if (result.isPresent()) {
            database.save(result.get()); // Сохраняем в БД!
            return result;
        }
        return searchNext(query);
    }
}

public class OmdbSearchHandler extends MovieSearchHandler {
    @Override
    public Optional<Movie> search(String query) {
        Optional<Movie> result = omdbApi.search(query);
        if (result.isPresent()) {
            database.save(result.get()); // Сохраняем в БД!
        }
        return result;
    }
}

// Использование:
// dbHandler.setNext(tmdbHandler);
// tmdbHandler.setNext(omdbHandler);
// Optional<Movie> movie = dbHandler.search("Inception");
```

**Преимущества:**
- Автоматическое обогащение БД при поиске
- Fallback между источниками
- Легко добавлять новые источники

### 3. Strategy Pattern - Стратегии завершения голосования

```java
/**
 * Стратегия определения завершения голосования
 */
public interface VotingCompletionStrategy {
    boolean isComplete(Map<String, Set<String>> participantLikes, int totalParticipants);
    List<String> getMatchedMovies(Map<String, Set<String>> participantLikes, int totalParticipants);
}

/**
 * Все участники должны лайкнуть один и тот же фильм (100%)
 */
public class UnanimousVotingStrategy implements VotingCompletionStrategy {
    @Override
    public boolean isComplete(Map<String, Set<String>> likes, int total) {
        Map<String, Integer> counts = countMovieLikes(likes);
        return counts.values().stream().anyMatch(count -> count == total);
    }
}

/**
 * Большинство участников (70%) должны лайкнуть один и тот же фильм
 */
public class MajorityVotingStrategy implements VotingCompletionStrategy {
    private static final double THRESHOLD = 0.70;
    
    @Override
    public boolean isComplete(Map<String, Set<String>> likes, int total) {
        Map<String, Integer> counts = countMovieLikes(likes);
        int requiredVotes = (int) Math.ceil(total * THRESHOLD);
        return counts.values().stream().anyMatch(count -> count >= requiredVotes);
    }
}
```

### 4. Transactional Outbox - Обогащение БД

```java
/**
 * Паттерн для надежного асинхронного обогащения БД
 */
public class EnrichmentService {
    
    /**
     * Шаг 1: Сохраняем ID фильмов в очередь
     */
    @Transactional
    public void enqueueMoviesForEnrichment(List<String> movieIds, String source) {
        for (String movieId : movieIds) {
            EnrichmentQueueItem item = new EnrichmentQueueItem();
            item.externalId = movieId;
            item.source = source; // "TMDB" или "OMDB"
            item.status = EnrichmentStatus.PENDING;
            item.persist();
        }
    }
    
    /**
     * Шаг 2: Асинхронная обработка очереди (Scheduled task)
     */
    @Scheduled(every = "10s")
    @Transactional
    public void processEnrichmentQueue() {
        List<EnrichmentQueueItem> pending = EnrichmentQueueItem
            .find("status = ?1 ORDER BY createdAt", EnrichmentStatus.PENDING)
            .page(0, 10)
            .list();
            
        for (EnrichmentQueueItem item : pending) {
            try {
                Movie movie = fetchFullMovieData(item.externalId, item.source);
                Movie.persist(movie);
                item.delete(); // Успешно - удаляем из очереди
            } catch (Exception e) {
                item.retryCount++;
                if (item.retryCount > 3) {
                    item.status = EnrichmentStatus.FAILED;
                }
                item.persist();
            }
        }
    }
}
```

**Преимущества:**
- Устойчивость к сбоям (resilience)
- Eventual consistency
- Idempotency (повторная обработка безопасна)

### 5. Factory Pattern - Создание комнат

```java
public class RoomFactory {
    
    public Room createRoom(
        String hostId, 
        RoomFilters filters, 
        VotingCompletionType completionType
    ) {
        VotingCompletionStrategy strategy = switch (completionType) {
            case UNANIMOUS -> new UnanimousVotingStrategy();
            case MAJORITY -> new MajorityVotingStrategy();
        };
        
        return new Room(
            UUID.randomUUID().toString(),
            hostId,
            filters,
            strategy
        );
    }
}
```

### 6. Rich Domain Model (DDD) - Room как Aggregate

```java
/**
 * Room - богатая доменная модель с инкапсулированной бизнес-логикой
 * Aggregate Root по DDD
 */
public class Room {
    private final String id;
    private final String hostId;
    private final VotingCompletionStrategy completionStrategy;
    
    // Value Objects
    private RoomFilters filters;
    private RoomState state; // WAITING, VOTING, COMPLETED
    
    // Entities
    private final List<Participant> participants = new CopyOnWriteArrayList<>();
    private VotingSession currentSession;
    
    // Бизнес-логика инкапсулирована
    
    public void addParticipant(String participantId) {
        if (state == RoomState.VOTING) {
            throw new IllegalStateException("Cannot join room during voting");
        }
        participants.add(new Participant(participantId));
    }
    
    public void startVoting() {
        if (participants.size() < 2) {
            throw new IllegalStateException("Need at least 2 participants");
        }
        state = RoomState.VOTING;
        currentSession = new VotingSession(participants, completionStrategy);
    }
    
    public void recordVote(String participantId, String movieId, boolean isLike) {
        if (state != RoomState.VOTING) {
            throw new IllegalStateException("Voting not started");
        }
        currentSession.recordVote(participantId, movieId, isLike);
        
        if (currentSession.isComplete()) {
            state = RoomState.COMPLETED;
        }
    }
    
    public boolean shouldBeDestroyed() {
        return participants.isEmpty() || 
               (state == RoomState.COMPLETED && allParticipantsLeft());
    }
}
```

**Принципы DDD:**
- Бизнес-логика в доменной модели, не в сервисах
- Инварианты защищены (нельзя голосовать до старта)
- Богатая модель вместо анемичной

## Архитектура слоев

### 1. Domain Layer (Доменный слой)

```
com.moviematcher.domain
├── model
│   ├── Room.java                    // Aggregate Root
│   ├── Participant.java             // Entity
│   ├── VotingSession.java           // Entity
│   ├── VotingRound.java             // Entity
│   ├── MovieSelection.java          // Value Object
│   └── RoomFilters.java             // Value Object
├── strategy
│   ├── VotingCompletionStrategy.java
│   ├── UnanimousVotingStrategy.java
│   └── MajorityVotingStrategy.java
└── service
    └── MovieSelectionService.java   // Domain Service
```

### 2. Application Layer (Прикладной слой)

```
com.moviematcher.application
├── service
│   ├── RoomApplicationService.java
│   └── EnrichmentService.java
└── dto
    ├── CreateRoomCommand.java
    ├── JoinRoomCommand.java
    └── VoteCommand.java
```

### 3. Infrastructure Layer (Инфраструктура)

```
com.moviematcher.infrastructure
├── persistence
│   ├── MovieRepository.java
│   └── EnrichmentQueueRepository.java
├── adapter
│   ├── MovieDataSource.java         // Interface
│   ├── DatabaseMovieDataSource.java
│   ├── TmdbApiDataSource.java
│   └── OmdbApiDataSource.java
├── api
│   ├── tmdb
│   │   ├── TmdbRestClient.java
│   │   └── TmdbApiAdapter.java
│   └── omdb
│       ├── OmdbRestClient.java
│       └── OmdbApiAdapter.java
└── messaging
    └── WebSocketBroadcaster.java
```

### 4. Presentation Layer (Представление)

```
com.moviematcher.presentation
├── rest
│   └── RoomResource.java
└── websocket
    └── RoomWebSocket.java
```

## Логика голосования (Round-Based System)

### Круговая система с чередованием

```java
public class VotingSession {
    private final List<Participant> participants;
    private final VotingCompletionStrategy completionStrategy;
    private int currentRound = 0;
    
    // Фильмы каждого участника
    private final Map<String, Queue<Movie>> participantMovieQueues = new HashMap<>();
    
    // Сначала показываем фильмы из поиска пользователей
    private final Queue<Movie> userSearchedMovies = new LinkedList<>();
    
    /**
     * Получить следующий фильм для показа
     * Логика:
     * 1. Сначала фильмы из поиска пользователей
     * 2. Потом чередование: User1, User2, User1, User2...
     */
    public Movie getNextMovie() {
        // Приоритет - пользовательский поиск
        if (!userSearchedMovies.isEmpty()) {
            return userSearchedMovies.poll();
        }
        
        // Чередование участников по кругу
        int participantIndex = currentRound % participants.size();
        Participant participant = participants.get(participantIndex);
        
        Queue<Movie> movies = participantMovieQueues.get(participant.getId());
        Movie movie = movies.poll();
        
        if (movie != null) {
            currentRound++;
        }
        
        return movie;
    }
    
    /**
     * Проверка дубликатов
     */
    private final Set<String> shownMovieIds = new HashSet<>();
    
    public boolean isMovieAlreadyShown(String movieId) {
        return !shownMovieIds.add(movieId);
    }
}
```

### Завершение раунда

```java
/**
 * Один раунд = показали по одному фильму от каждого участника
 */
public class VotingRound {
    private final int roundNumber;
    private final Map<String, Movie> participantMovies = new HashMap<>();
    private final Map<String, Map<String, Boolean>> votes = new HashMap<>();
    
    public boolean isComplete() {
        return participantMovies.size() == expectedParticipants;
    }
    
    public void addMovie(String participantId, Movie movie) {
        participantMovies.put(participantId, movie);
    }
}
```

## Модель данных БД (только для фильмов!)

### Основные таблицы

```sql
-- Фильмы и сериалы
CREATE TABLE movies (
    id BIGSERIAL PRIMARY KEY,
    imdb_id VARCHAR(20) UNIQUE NOT NULL,
    tmdb_id INTEGER,
    
    -- Названия (русский обязательно!)
    title_ru VARCHAR(500) NOT NULL,      -- Название на русском
    title_en VARCHAR(500) NOT NULL,      -- Оригинальное название
    
    -- Описание
    plot_ru TEXT,                        -- Сюжет на русском
    
    -- Базовая информация
    type VARCHAR(20) NOT NULL,           -- 'movie' или 'series'
    year INTEGER,
    release_date DATE,
    runtime INTEGER,                     -- минуты
    
    -- Рейтинги
    imdb_rating DECIMAL(3,1),
    metacritic_score INTEGER,
    
    -- Постеры
    poster_url TEXT,
    backdrop_url TEXT,
    
    -- Люди
    director VARCHAR(500),
    actors TEXT,                         -- JSON массив или через связь
    country VARCHAR(200),
    
    -- Метаданные
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    
    -- Уникальность по комбинации полей
    CONSTRAINT unique_movie UNIQUE (title_en, year, director, country)
);

-- Жанры
CREATE TABLE genres (
    id SERIAL PRIMARY KEY,
    name_ru VARCHAR(100) NOT NULL,
    name_en VARCHAR(100) NOT NULL
);

CREATE TABLE movie_genres (
    movie_id BIGINT REFERENCES movies(id),
    genre_id INTEGER REFERENCES genres(id),
    PRIMARY KEY (movie_id, genre_id)
);

-- Очередь обогащения (Transactional Outbox)
CREATE TABLE enrichment_queue (
    id BIGSERIAL PRIMARY KEY,
    external_id VARCHAR(50) NOT NULL,    -- TMDB ID или IMDB ID
    source VARCHAR(20) NOT NULL,         -- 'TMDB' или 'OMDB'
    status VARCHAR(20) NOT NULL,         -- 'PENDING', 'PROCESSING', 'FAILED'
    retry_count INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP
);

CREATE INDEX idx_enrichment_status ON enrichment_queue(status, created_at);
```

### Индексы для производительности

```sql
-- Поиск по фильтрам
CREATE INDEX idx_movies_year ON movies(year);
CREATE INDEX idx_movies_type ON movies(type);
CREATE INDEX idx_movies_rating ON movies(imdb_rating);

-- Полнотекстовый поиск по названиям
CREATE INDEX idx_movies_title_ru_gin ON movies USING gin(to_tsvector('russian', title_ru));
CREATE INDEX idx_movies_title_en_gin ON movies USING gin(to_tsvector('english', title_en));
```

## WebSocket Protocol

### События от клиента → сервер

```typescript
// Создание комнаты
{
  type: "CREATE_ROOM",
  hostId: string,
  filters: MovieFilters,
  completionType: "UNANIMOUS" | "MAJORITY"  // 100% или 70%
}

// Присоединение к комнате
{
  type: "JOIN_ROOM",
  roomId: string,
  participantId: string
}

// Установка фильтров участником
{
  type: "SET_FILTERS",
  roomId: string,
  participantId: string,
  filters: MovieFilters
}

// Поиск фильма
{
  type: "SEARCH_MOVIE",
  roomId: string,
  participantId: string,
  query: string
}

// Добавление фильма в свою выборку
{
  type: "ADD_MOVIE_TO_SELECTION",
  roomId: string,
  participantId: string,
  movieId: string
}

// Готовность к голосованию
{
  type: "READY_TO_VOTE",
  roomId: string,
  participantId: string
}

// Голос (свайп вверх/вниз)
{
  type: "VOTE",
  roomId: string,
  participantId: string,
  movieId: string,
  isLike: boolean  // true = swipe up, false = swipe down
}

// Выход из комнаты
{
  type: "LEAVE_ROOM",
  roomId: string,
  participantId: string
}
```

### События от сервера → клиент

```typescript
// Комната создана
{
  type: "ROOM_CREATED",
  room: {
    id: string,
    hostId: string,
    qrCodeUrl: string,  // QR code для присоединения
    shareUrl: string
  }
}

// Участник присоединился
{
  type: "PARTICIPANT_JOINED",
  participantId: string,
  totalParticipants: number
}

// Участник ушел
{
  type: "PARTICIPANT_LEFT",
  participantId: string,
  totalParticipants: number
}

// Участник готов к голосованию
{
  type: "PARTICIPANT_READY",
  participantId: string,
  readyCount: number,
  totalParticipants: number
}

// Голосование началось
{
  type: "VOTING_STARTED",
  message: "Голосование началось! Комната закрыта для новых участников"
}

// Попытка присоединиться во время голосования
{
  type: "ROOM_LOCKED",
  message: "Голосование уже идет, присоединиться нельзя"
}

// Новый фильм для голосования
{
  type: "NEW_MOVIE",
  movie: MovieData,
  roundNumber: number,
  fromParticipant: string  // чей это фильм из фильтра
}

// Голос записан
{
  type: "VOTE_RECORDED",
  participantId: string,
  movieId: string
}

// Раунд завершен
{
  type: "ROUND_COMPLETED",
  roundNumber: number,
  commonLikes: Movie[]  // фильмы, которые все лайкнули в этом раунде
}

// Совпадение найдено!
{
  type: "MATCH_FOUND",
  matchedMovies: Movie[],  // может быть несколько
  completionType: "UNANIMOUS" | "MAJORITY"
}

// Голосование завершено
{
  type: "VOTING_ENDED",
  matchedMovies: Movie[],
  allLikes: Map<participantId, Movie[]>
}
```

## SOLID Principles в действии

### Single Responsibility Principle
- `RoomService` → управление жизненным циклом комнат (создание, удаление)
- `VotingService` → логика голосования
- `MovieSelectionService` → подбор фильмов по фильтрам
- `EnrichmentService` → обогащение БД
- `MovieSearchService` → поиск фильмов (Chain of Responsibility)

### Open/Closed Principle
- Новые источники данных → новый `MovieDataSource`
- Новые стратегии голосования → новый `VotingCompletionStrategy`
- Не меняем существующий код, расширяем через интерфейсы

### Liskov Substitution Principle
- Все `MovieDataSource` взаимозаменяемы
- Все `VotingCompletionStrategy` работают одинаково для клиента

### Interface Segregation Principle
```java
// Разделенные интерфейсы вместо одного большого
interface MovieProvider {
    List<Movie> findByFilters(MovieFilters filters);
}

interface MovieSearcher {
    Optional<Movie> searchByTitle(String title);
}

interface MovieEnricher {
    void enrichDatabase(String externalId, String source);
}
```

### Dependency Inversion Principle
- Высокоуровневые модули зависят от абстракций
- `RoomService` зависит от `MovieSelectionService` (интерфейс)
- REST clients скрыты за адаптерами

## Cleanup Strategy для комнат

```java
@ApplicationScoped
public class RoomCleanupService {
    
    private final Map<String, Room> rooms;
    
    /**
     * Scheduled cleanup каждую минуту
     */
    @Scheduled(every = "1m")
    public void cleanupAbandonedRooms() {
        rooms.entrySet().removeIf(entry -> {
            Room room = entry.getValue();
            
            // Удаляем если:
            // 1. Нет участников
            // 2. Голосование завершено и все вышли
            // 3. Комната старше 24 часов
            return room.shouldBeDestroyed() || room.isOlderThan(Duration.ofHours(24));
        });
    }
    
    /**
     * Когда последний участник выходит
     */
    public void onParticipantLeft(String roomId, String participantId) {
        Room room = rooms.get(roomId);
        room.removeParticipant(participantId);
        
        if (room.getParticipants().isEmpty()) {
            rooms.remove(roomId);
            log.info("Room {} destroyed - no participants", roomId);
        }
    }
}
```

## Масштабирование в будущем

Текущая реализация: `ConcurrentHashMap` в памяти одного инстанса

Для горизонтального масштабирования (multiple instances):
- **Redis** для хранения комнат
- **Redis Pub/Sub** для WebSocket broadcast между инстансами
- **Sticky sessions** на load balancer (по roomId)

## Тестирование

### Unit Tests
```java
// Domain models
@Test
void room_shouldNotAllowJoiningDuringVoting() {
    Room room = new Room(/*...*/);
    room.startVoting();
    
    assertThrows(IllegalStateException.class, 
        () -> room.addParticipant("newUser"));
}

// Strategies
@Test
void unanimousStrategy_shouldRequireAllParticipants() {
    VotingCompletionStrategy strategy = new UnanimousVotingStrategy();
    // ...
}
```

### Integration Tests
```java
@QuarkusTest
class MovieRepositoryTest {
    @Test
    void shouldFindMoviesByFilters() {
        // Test DB queries
    }
}
```

### Contract Tests
- TMDB API contract validation
- OMDB API contract validation

## Следующие шаги

1. ✅ Архитектурный документ
2. Создать базовые интерфейсы (MovieDataSource, VotingCompletionStrategy)
3. Реализовать Adapter pattern для источников данных
4. Реализовать Chain of Responsibility для поиска
5. Создать EnrichmentService
6. Рефакторинг Room в Rich Domain Model
7. Реализовать круговую систему голосования
8. Добавить TMDB API
9. Обновить Liquibase миграции
10. Создать Flutter приложение
