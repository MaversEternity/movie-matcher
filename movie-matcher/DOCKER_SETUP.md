# Docker Setup для Movie Matcher

## Быстрый старт

### 1. Запуск приложения

```bash
# Из корневой директории проекта
cd /Users/dante/ZedProjects/movie-matcher/movie-matcher

# Запустить PostgreSQL и backend-java
docker-compose up -d
```

### 2. Проверка статуса

```bash
# Проверить запущенные контейнеры
docker-compose ps

# Посмотреть логи
docker-compose logs -f backend-java
docker-compose logs -f postgres
```

### 3. Доступ к приложению

- **Backend API**: http://localhost:3000
- **Health Check**: http://localhost:3000/health
- **PostgreSQL**: localhost:5432
  - Database: `movie_matcher_dev`
  - User: `postgres`
  - Password: `postgres`

### 4. Frontend (запускается отдельно локально)

```bash
cd frontend
npm install
npm run dev
```

Frontend будет доступен на http://localhost:8080

## Управление

### Остановка сервисов

```bash
# Остановить контейнеры (данные сохраняются)
docker-compose stop

# Остановить и удалить контейнеры (данные сохраняются)
docker-compose down
```

### Перезапуск

```bash
# Перезапустить все сервисы
docker-compose restart

# Перезапустить только backend
docker-compose restart backend-java
```

### Пересборка после изменений кода

```bash
# Пересобрать и перезапустить backend
docker-compose up -d --build backend-java

# Полная пересборка всего
docker-compose up -d --build
```

## Данные PostgreSQL

### Данные сохраняются между перезапусками

Docker volume `postgres_data` сохраняет все данные БД. Даже после:
- `docker-compose down`
- `docker-compose stop`
- Перезагрузки компьютера

Данные **НЕ удаляются** автоматически!

### Просмотр данных

```bash
# Подключиться к PostgreSQL
docker exec -it movie-matcher-postgres psql -U postgres -d movie_matcher_dev

# Внутри psql:
\dt                    # Список таблиц
SELECT * FROM movies LIMIT 10;
\q                     # Выход
```

### Полная очистка данных (если нужно начать с нуля)

```bash
# Остановить и удалить контейнеры
docker-compose down

# Удалить volume с данными
docker volume rm movie-matcher_postgres_data

# Запустить заново (миграции выполнятся автоматически)
docker-compose up -d
```

### Резервное копирование

```bash
# Создать бэкап
docker exec movie-matcher-postgres pg_dump -U postgres movie_matcher_dev > backup.sql

# Восстановить из бэкапа
cat backup.sql | docker exec -i movie-matcher-postgres psql -U postgres -d movie_matcher_dev
```

## Логи

### Просмотр логов

```bash
# Все логи
docker-compose logs

# Только backend
docker-compose logs backend-java

# Только БД
docker-compose logs postgres

# Следить за логами в реальном времени
docker-compose logs -f backend-java
```

### Очистка логов

```bash
# Очистить логи всех контейнеров
docker-compose down
docker system prune -f
docker-compose up -d
```

## Переменные окружения

### Настройка OMDB API Key

Создайте файл `.env` в корне проекта:

```bash
OMDB_API_KEY=your_actual_api_key_here
```

Или экспортируйте переменную:

```bash
export OMDB_API_KEY=your_actual_api_key_here
docker-compose up -d
```

### Другие переменные

В `docker-compose.yml` можно изменить:
- `DB_USERNAME` - имя пользователя БД
- `DB_PASSWORD` - пароль БД
- `POSTGRES_DB` - имя базы данных
- `JAVA_OPTS` - параметры JVM

## Debug

### Подключение debugger к Java приложению

Порт 5005 проброшен для remote debugging:

**IntelliJ IDEA:**
1. Run → Edit Configurations
2. Add New Configuration → Remote JVM Debug
3. Host: `localhost`, Port: `5005`
4. Start debugging

**VS Code:**
```json
{
  "type": "java",
  "request": "attach",
  "name": "Attach to Docker",
  "hostName": "localhost",
  "port": 5005
}
```

## Troubleshooting

### Проблема: Backend не запускается

```bash
# Проверить логи
docker-compose logs backend-java

# Проверить, что PostgreSQL готов
docker-compose logs postgres | grep "ready to accept connections"

# Перезапустить backend
docker-compose restart backend-java
```

### Проблема: Порт занят

```bash
# Проверить, что занимает порт
lsof -i :3000
lsof -i :5432

# Остановить процесс или изменить порт в docker-compose.yml
```

### Проблема: Нехватка памяти

Увеличьте лимиты в `docker-compose.yml`:

```yaml
backend-java:
  environment:
    JAVA_OPTS: "-Xmx1g -Xms512m"  # Увеличить с 512m до 1g
```

### Проблема: Миграции не применяются

```bash
# Проверить статус Liquibase в логах
docker-compose logs backend-java | grep Liquibase

# Пересоздать контейнер
docker-compose down
docker-compose up -d --build
```

### Полная переустановка

```bash
# Остановить всё
docker-compose down

# Удалить контейнеры, сети, volumes
docker-compose down -v

# Удалить образы
docker rmi movie-matcher-backend-java

# Очистить Docker cache
docker system prune -a -f

# Запустить заново
docker-compose up -d --build
```

## Производительность

### Мониторинг ресурсов

```bash
# Использование ресурсов контейнерами
docker stats

# Размер образов
docker images | grep movie-matcher

# Размер volumes
docker volume ls
docker system df -v
```

### Оптимизация

1. **Ограничить память PostgreSQL:**
```yaml
postgres:
  deploy:
    resources:
      limits:
        memory: 512M
```

2. **Настроить connection pool:**
В `application.properties`:
```properties
quarkus.datasource.jdbc.max-size=10
quarkus.datasource.jdbc.min-size=2
```

## Полезные команды

```bash
# Войти в контейнер backend
docker exec -it movie-matcher-backend-java sh

# Войти в контейнер PostgreSQL
docker exec -it movie-matcher-postgres sh

# Проверить health check
curl http://localhost:3000/health

# Посмотреть метрики
curl http://localhost:3000/q/metrics

# Список volumes
docker volume ls

# Информация о volume
docker volume inspect movie-matcher_postgres_data
```

## Production deployment

Для production используйте отдельный `docker-compose.prod.yml`:

```yaml
services:
  backend-java:
    environment:
      QUARKUS_PROFILE: prod
      JAVA_OPTS: "-Xmx1g -Xms512m -XX:+UseG1GC"
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 1G
```

Запуск:
```bash
docker-compose -f docker-compose.prod.yml up -d
```
