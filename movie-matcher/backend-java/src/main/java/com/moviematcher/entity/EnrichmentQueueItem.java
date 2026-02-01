package com.moviematcher.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * Entity для очереди обогащения БД (Transactional Outbox Pattern)
 *
 * Хранит ID фильмов, которые нужно загрузить из внешних API
 * Обрабатывается асинхронно через scheduled task
 */
@Entity
@Table(name = "enrichment_queue")
public class EnrichmentQueueItem extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /**
     * Внешний ID фильма (IMDB ID или TMDB ID)
     */
    @Column(name = "external_id", nullable = false, length = 50)
    public String externalId;

    /**
     * Источник данных: "TMDB" или "OMDB"
     */
    @Column(nullable = false, length = 20)
    public String source;

    /**
     * Статус обработки
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public EnrichmentStatus status = EnrichmentStatus.PENDING;

    /**
     * Количество попыток обработки
     */
    @Column(name = "retry_count", nullable = false)
    public Integer retryCount = 0;

    /**
     * Сообщение об ошибке (если есть)
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    public String errorMessage;

    /**
     * Время создания записи
     */
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    /**
     * Время обработки
     */
    @Column(name = "processed_at")
    public LocalDateTime processedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Статус обработки элемента очереди
     */
    public enum EnrichmentStatus {
        PENDING,      // Ожидает обработки
        PROCESSING,   // В процессе обработки
        COMPLETED,    // Успешно обработан
        FAILED        // Ошибка после всех попыток
    }
}
