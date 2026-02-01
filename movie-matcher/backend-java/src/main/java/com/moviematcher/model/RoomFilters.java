package com.moviematcher.model;

/**
 * Алиас для MovieFilters - просто переэкспортируем
 * Используется в presentation layer для обратной совместимости
 */
public class RoomFilters extends com.moviematcher.domain.model.MovieFilters {

    public RoomFilters(
        String genre,
        Integer yearFrom,
        Integer yearTo,
        java.math.BigDecimal minRating,
        String type
    ) {
        super(genre, yearFrom, yearTo, minRating, type);
    }
}
