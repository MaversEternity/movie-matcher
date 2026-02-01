package com.moviematcher.service;

import com.moviematcher.entity.EnrichmentQueueItem;
import com.moviematcher.entity.EnrichmentQueueItem.EnrichmentStatus;
import com.moviematcher.entity.Movie;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Сервис для обогащения БД фильмами из внешних источников
 *
 * Использует паттерн Transactional Outbox:
 * 1. Сохраняем ID фильмов в очередь (enrichment_queue)
 * 2. Асинхронно обрабатываем очередь
 * 3. Получаем полную информацию о фильме из API
 * 4. Сохраняем в movies таблицу
 * 5. Удаляем из очереди
 *
 * Преимущества:
 * - Eventual Consistency
 * - Устойчивость к сбоям (retry mechanism)
 * - Idempotency (повторная обработка безопасна)
 */
@ApplicationScoped
public class EnrichmentService {

    private static final Logger log = Logger.getLogger(EnrichmentService.class);

    private static final int MAX_RETRY_COUNT = 3;
    private static final int BATCH_SIZE = 10;

    /**
     * Добавить фильм в очередь обогащения
     *
     * @param externalId внешний ID (IMDB или TMDB)
     * @param source источник ("TMDB" или "OMDB")
     */
    @Transactional
    public void enqueueMovieForEnrichment(String externalId, String source) {
        // Проверяем, нет ли уже в очереди
        long count = EnrichmentQueueItem.count(
            "externalId = ?1 and source = ?2 and status != ?3",
            externalId,
            source,
            EnrichmentStatus.FAILED
        );

        if (count > 0) {
            log.debugf(
                "Movie {} from {} already in queue, skipping",
                externalId,
                source
            );
            return;
        }

        // Проверяем, нет ли уже в БД
        if (Movie.findByImdbId(externalId) != null) {
            log.debugf(
                "Movie {} already exists in database, skipping",
                externalId
            );
            return;
        }

        EnrichmentQueueItem item = new EnrichmentQueueItem();
        item.externalId = externalId;
        item.source = source;
        item.status = EnrichmentStatus.PENDING;
        item.persist();

        log.infof(
            "Enqueued movie {} from {} for enrichment",
            externalId,
            source
        );
    }

    /**
     * Массовое добавление в очередь
     */
    @Transactional
    public void enqueueMoviesForEnrichment(
        List<String> externalIds,
        String source
    ) {
        for (String externalId : externalIds) {
            enqueueMovieForEnrichment(externalId, source);
        }
        log.infof(
            "Enqueued {} movies from {} for enrichment",
            externalIds.size(),
            source
        );
    }

    /**
     * Scheduled task для обработки очереди
     * Выполняется каждые 10 секунд
     */
    @Scheduled(every = "10s")
    @Transactional
    public void processEnrichmentQueue() {
        List<EnrichmentQueueItem> pending = EnrichmentQueueItem.<
                EnrichmentQueueItem
            >find("status = ?1 ORDER BY createdAt", EnrichmentStatus.PENDING)
            .page(0, BATCH_SIZE)
            .list();

        if (pending.isEmpty()) {
            return;
        }

        log.infof("Processing {} items from enrichment queue", pending.size());

        for (EnrichmentQueueItem item : pending) {
            processQueueItem(item);
        }
    }

    /**
     * Обработка одного элемента очереди
     */
    private void processQueueItem(EnrichmentQueueItem item) {
        try {
            item.status = EnrichmentStatus.PROCESSING;
            item.persist();

            log.debugf(
                "Processing enrichment item: {} from {}",
                item.externalId,
                item.source
            );

            // Проверяем, не добавили ли фильм уже
            Movie existing = Movie.findByImdbId(item.externalId);
            if (existing != null) {
                log.infof(
                    "Movie {} already exists, removing from queue",
                    item.externalId
                );
                item.delete();
                return;
            }

            // Загружаем полную информацию о фильме
            // TODO: использовать соответствующий API клиент в зависимости от source
            // Пока это заглушка, реальная реализация будет через API адаптеры

            item.status = EnrichmentStatus.COMPLETED;
            item.processedAt = LocalDateTime.now();
            item.delete(); // Удаляем успешно обработанный элемент

            log.infof(
                "Successfully processed enrichment item: {}",
                item.externalId
            );
        } catch (Exception e) {
            log.errorf(
                "Error processing enrichment item: {}",
                item.externalId,
                e
            );

            item.retryCount++;
            item.errorMessage = e.getMessage();

            if (item.retryCount >= MAX_RETRY_COUNT) {
                item.status = EnrichmentStatus.FAILED;
                log.warnf(
                    "Enrichment item {} failed after {} retries",
                    item.externalId,
                    MAX_RETRY_COUNT
                );
            } else {
                item.status = EnrichmentStatus.PENDING;
                log.infof(
                    "Enrichment item {} will be retried ({}/{})",
                    item.externalId,
                    item.retryCount,
                    MAX_RETRY_COUNT
                );
            }

            item.persist();
        }
    }

    /**
     * Получить статистику очереди обогащения
     */
    public EnrichmentStats getStats() {
        long pending = EnrichmentQueueItem.count(
            "status",
            EnrichmentStatus.PENDING
        );
        long processing = EnrichmentQueueItem.count(
            "status",
            EnrichmentStatus.PROCESSING
        );
        long failed = EnrichmentQueueItem.count(
            "status",
            EnrichmentStatus.FAILED
        );

        return new EnrichmentStats(pending, processing, failed);
    }

    public record EnrichmentStats(long pending, long processing, long failed) {}
}
