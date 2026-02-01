package com.moviematcher.client.tmdb;

/**
 * Жанр из TMDB API
 */
public record TmdbGenre(
    Integer id,
    String name
) {}
