package com.moviematcher.client;

import com.moviematcher.model.MovieData;
import com.moviematcher.model.RoomFilters;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@ApplicationScoped
public class OmdbClient {

    @ConfigProperty(name = "omdb.api.key")
    String apiKey;

    @RestClient
    final OmdbRestClient restClient;

    public record MoviesPage(List<MovieData> movies, boolean hasMore) {}

    public Uni<MoviesPage> fetchMoviesByPage(RoomFilters filters, int page) {
        String searchTerm = filters.genre() != null ? filters.genre() : "movie";
        Integer year = filters.yearFrom();

        return restClient
            .search(apiKey, searchTerm, "movie", page, year)
            .onItem()
            .transformToUni(searchResponse -> {
                if (!"True".equals(searchResponse.response())) {
                    String error =
                        searchResponse.error() != null
                            ? searchResponse.error()
                            : "Unknown error";
                    log.error("OMDB error: {}", error);
                    return Uni.createFrom().failure(
                        new RuntimeException("OMDB error: " + error)
                    );
                }

                List<OmdbSearchResult> searchResults =
                    searchResponse.search() != null
                        ? searchResponse.search()
                        : List.of();

                int total =
                    searchResponse.totalResults() != null
                        ? Integer.parseInt(searchResponse.totalResults())
                        : 0;
                boolean hasMore = (page * 10) < total;

                List<String> movieIds = searchResults
                    .stream()
                    .filter(r -> "movie".equals(r.type()))
                    .map(OmdbSearchResult::imdbId)
                    .toList();

                if (movieIds.isEmpty()) {
                    return Uni.createFrom().item(
                        new MoviesPage(List.of(), hasMore)
                    );
                }

                // Fetch details for all movies in parallel
                List<Uni<MovieData>> detailUnis = movieIds
                    .stream()
                    .map(this::fetchMovieDetails)
                    .toList();

                return Uni.combine()
                    .all()
                    .unis(detailUnis)
                    .combinedWith(results -> {
                        List<MovieData> movies = results
                            .stream()
                            .filter(obj -> obj != null)
                            .map(obj -> (MovieData) obj)
                            .collect(Collectors.toList());

                        // Apply year range filter
                        if (
                            filters.yearFrom() != null &&
                            filters.yearTo() != null
                        ) {
                            movies = movies
                                .stream()
                                .filter(m -> {
                                    try {
                                        String yearStr = m.year().split("–")[0];
                                        int movieYear = Integer.parseInt(
                                            yearStr
                                        );
                                        return (
                                            movieYear >= filters.yearFrom() &&
                                            movieYear <= filters.yearTo()
                                        );
                                    } catch (Exception e) {
                                        return true;
                                    }
                                })
                                .collect(Collectors.toList());
                        }

                        log.info(
                            "Fetched {} movies from page {}",
                            movies.size(),
                            page
                        );
                        return new MoviesPage(movies, hasMore);
                    });
            });
    }

    private Uni<MovieData> fetchMovieDetails(String imdbId) {
        return restClient
            .getDetails(apiKey, imdbId, "short")
            .onItem()
            .transform(detail -> {
                if (!"True".equals(detail.response())) {
                    return null;
                }

                return new MovieData(
                    detail.title(),
                    detail.year(),
                    "N/A".equals(detail.rated()) ? "" : detail.rated(),
                    "N/A".equals(detail.runtime()) ? "" : detail.runtime(),
                    "N/A".equals(detail.poster()) ? "" : detail.poster(),
                    "N/A".equals(detail.director()) ? "" : detail.director(),
                    "N/A".equals(detail.actors()) ? "" : detail.actors(),
                    "N/A".equals(detail.plot()) ? "" : detail.plot(),
                    "N/A".equals(detail.country()) ? "" : detail.country(),
                    detail.genre(),
                    "N/A".equals(detail.imdbRating())
                        ? ""
                        : detail.imdbRating(),
                    detail.imdbId()
                );
            })
            .onFailure()
            .recoverWithItem((MovieData) null);
    }
}
