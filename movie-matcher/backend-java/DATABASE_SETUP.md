## Настройка PostgreSQL для Movie Matcher

## Быстрый старт

### 1. Установка PostgreSQL

**macOS (Homebrew):**
```bash
brew install postgresql@16
brew services start postgresql@16
```

**Ubuntu/Debian:**
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

**Docker (рекомендуется для разработки):**
```bash
docker run --name movie-matcher-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=movie_matcher_dev \
  -p 5432:5432 \
  -d postgres:16-alpine
```

### 2. Создание базы данных

**Вариант 1: Через psql**
```bash
# Подключение к PostgreSQL
psql -U postgres

# Создание баз данных
CREATE DATABASE movie_matcher_dev;
CREATE DATABASE movie_matcher_test;
CREATE DATABASE movie_matcher;

# Выход
\q
```

**Вариант 2: Через Docker**
```bash
docker exec -it movie-matcher-postgres psql -U postgres -c "CREATE DATABASE movie_matcher_dev;"
docker exec -it movie-matcher-postgres psql -U postgres -c "CREATE DATABASE movie_matcher_test;"
```

### 3. Настройка переменных окружения

Создайте/обновите файл `.env` в `backend-java/`:

```bash
# OMDB API Key
OMDB_API_KEY=your_api_key_here

# Database Configuration
DB_USERNAME=postgres
DB_PASSWORD=postgres
DB_URL=jdbc:postgresql://localhost:5432/movie_matcher_dev
```

### 4. Запуск приложения

При первом запуске Liquibase автоматически выполнит все миграции:

```bash
cd backend-java
mvn quarkus:dev
```

Вы увидите в логах:
```
Liquibase: Successfully acquired change log lock
Liquibase: Reading from DATABASECHANGELOG
Liquibase: Running Changeset: db/changesets/001-create-base-tables.xml::001-create-movies-table
...
Liquibase: Successfully released change log lock
```

## Структура базы данных

### Основные таблицы

**movies** - фильмы и сериалы (1M+ записей)
- Индексы: imdb_id, year, rating, type
- Поддержка полнотекстового поиска

**genres** - справочник жанров (18 жанров)
**countries** - справочник стран (15+ стран)
**languages** - справочник языков (12+ языков)

### Связующие таблицы

**movie_genres** - связь фильмов и жанров (M:N)
**movie_countries** - связь фильмов и стран (M:N)
**movie_languages** - связь фильмов и языков (M:N)

### Люди и роли

**people** - актеры, режиссеры, сценаристы
**movie_credits** - роли людей в фильмах
- Типы ролей: actor, director, writer, producer

### Дополнительные таблицы

**studios** - кинокомпании
**keywords** - ключевые слова/теги
**age_ratings** - возрастные рейтинги
**series_seasons** - сезоны сериалов

### Пользовательские данные

**user_ratings** - оценки пользователей (1-10)
**user_watchlist** - список желаемого к просмотру

## Миграции

### Просмотр статуса миграций

```bash
mvn liquibase:status
```

### Откат миграции

```bash
mvn liquibase:rollback -Dliquibase.rollbackCount=1
```

### Создание новой миграции

1. Создайте новый файл в `src/main/resources/db/changesets/`:
```bash
touch src/main/resources/db/changesets/006-add-new-feature.xml
```

2. Добавьте changeset:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.20.xsd">

    <changeSet id="033-add-new-feature" author="your-name">
        <!-- Ваши изменения -->
    </changeSet>

</databaseChangeLog>
```

3. Добавьте в `changeLog.xml`:
```xml
<include file="db/changesets/006-add-new-feature.xml"/>
```

## Проверка работы

### Подключение к базе

```bash
psql -U postgres -d movie_matcher_dev
```

### Полезные SQL команды

```sql
-- Список таблиц
\dt

-- Структура таблицы
\d movies

-- Подсчет фильмов
SELECT COUNT(*) FROM movies;

-- Список жанров
SELECT * FROM genres;

-- Фильмы с жанрами
SELECT m.title, m.year, g.name as genre
FROM movies m
JOIN movie_genres mg ON m.id = mg.movie_id
JOIN genres g ON mg.genre_id = g.id
LIMIT 10;

-- Топ фильмов по рейтингу
SELECT title, year, imdb_rating
FROM movies
WHERE imdb_rating IS NOT NULL
ORDER BY imdb_rating DESC, imdb_votes DESC
LIMIT 10;
```

## Производительность

### Анализ запросов

```sql
EXPLAIN ANALYZE
SELECT * FROM movies 
WHERE year >= 2020 AND imdb_rating >= 7.0
ORDER BY imdb_rating DESC
LIMIT 50;
```

### Создание дополнительных индексов (если нужно)

```sql
CREATE INDEX idx_movies_custom ON movies(year, imdb_rating) 
WHERE type = 'movie';
```

### Vacuum (очистка)

```sql
VACUUM ANALYZE movies;
```

## Резервное копирование

### Создание бэкапа

```bash
pg_dump -U postgres movie_matcher_dev > backup.sql
```

### Восстановление из бэкапа

```bash
psql -U postgres movie_matcher_dev < backup.sql
```

### Docker бэкап

```bash
docker exec movie-matcher-postgres pg_dump -U postgres movie_matcher_dev > backup.sql
```

## Troubleshooting

### Ошибка: "database does not exist"

Создайте базу данных:
```bash
createdb -U postgres movie_matcher_dev
```

### Ошибка: "password authentication failed"

Проверьте пароль в `.env` и `pg_hba.conf`

### Ошибка: "Liquibase lock"

Удалите блокировку:
```sql
DELETE FROM DATABASECHANGELOGLOCK;
```

### Сброс всех данных

```bash
# Остановить приложение
# Удалить БД
dropdb -U postgres movie_matcher_dev

# Создать заново
createdb -U postgres movie_matcher_dev

# Запустить приложение - миграции выполнятся автоматически
mvn quarkus:dev
```

## Docker Compose (опционально)

Создайте `docker-compose.yml` в корне проекта:

```yaml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    container_name: movie-matcher-postgres
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_DB: movie_matcher_dev
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

Запуск:
```bash
docker-compose up -d
```

## Мониторинг

### Активные подключения

```sql
SELECT count(*) FROM pg_stat_activity 
WHERE datname = 'movie_matcher_dev';
```

### Размер базы данных

```sql
SELECT pg_size_pretty(pg_database_size('movie_matcher_dev'));
```

### Размер таблиц

```sql
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```
