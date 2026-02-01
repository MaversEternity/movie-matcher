package com.moviematcher.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value Object для фильтров подбора фильмов
 *
 * Immutable по DDD принципам
 */
public class MovieFilters {

    private final String genre;
    private final Integer yearFrom;
    private final Integer yearTo;
    private final BigDecimal minRating;
    private final String type; // "movie" или "series"

    protected MovieFilters(
        String genre,
        Integer yearFrom,
        Integer yearTo,
        BigDecimal minRating,
        String type
    ) {
        this.genre = genre;
        this.yearFrom = yearFrom;
        this.yearTo =
            yearTo != null
                ? yearTo
                : (yearFrom != null ? LocalDate.now().getYear() : null);
        this.minRating = minRating;
        this.type = type != null && !type.isBlank() ? type : "movie";

        validate();
    }

    private void validate() {
        if (yearFrom != null && yearTo != null && yearFrom > yearTo) {
            throw new IllegalArgumentException(
                "yearFrom cannot be greater than yearTo"
            );
        }

        if (minRating != null) {
            if (
                minRating.compareTo(BigDecimal.ZERO) < 0 ||
                minRating.compareTo(BigDecimal.TEN) > 0
            ) {
                throw new IllegalArgumentException(
                    "minRating must be between 0 and 10"
                );
            }
        }
    }

    /**
     * Фабричный метод - создание без фильтров
     */
    public static MovieFilters noFilter() {
        return new MovieFilters(null, null, null, null, "movie");
    }

    /**
     * Builder для удобного создания
     */
    public static Builder builder() {
        return new Builder();
    }

    public boolean hasAnyFilter() {
        return genre != null || yearFrom != null || minRating != null;
    }

    // Геттеры
    public String getGenre() {
        return genre;
    }

    public Integer getYearFrom() {
        return yearFrom;
    }

    public Integer getYearTo() {
        return yearTo;
    }

    public BigDecimal getMinRating() {
        return minRating;
    }

    public String getType() {
        return type;
    }

    public static class Builder {

        private String genre;
        private Integer yearFrom;
        private Integer yearTo;
        private BigDecimal minRating;
        private String type = "movie";

        public Builder genre(String genre) {
            this.genre = genre;
            return this;
        }

        public Builder yearFrom(Integer yearFrom) {
            this.yearFrom = yearFrom;
            return this;
        }

        public Builder yearTo(Integer yearTo) {
            this.yearTo = yearTo;
            return this;
        }

        public Builder minRating(BigDecimal minRating) {
            this.minRating = minRating;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public MovieFilters build() {
            return new MovieFilters(genre, yearFrom, yearTo, minRating, type);
        }
    }
}
