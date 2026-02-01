package com.moviematcher.client.tmdb;

import java.util.List;

/**
 * Credits (актеры и съемочная группа) из TMDB API
 */
public record TmdbCreditsResponse(
    List<TmdbCastMember> cast,
    List<TmdbCrewMember> crew
) {}
