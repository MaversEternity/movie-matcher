package com.moviematcher.repository;

import com.moviematcher.entity.Movie;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@ApplicationScoped
public class MovieRepository implements PanacheRepository<Movie> {

    private final EntityManager em;

    /**
     * Universal method to find movies by all possible filters
     * Uses Criteria API for 100% protection against SQL injection
     */
    public MovieSearchResult findByFilters(MovieFilterCriteria criteria) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        // Query for movies
        CriteriaQuery<Movie> query = cb.createQuery(Movie.class);
        Root<Movie> movie = query.from(Movie.class);

        // Count query for total
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Movie> countRoot = countQuery.from(Movie.class);

        // Build predicates (WHERE conditions)
        List<Predicate> predicates = buildPredicates(cb, movie, criteria);
        List<Predicate> countPredicates = buildPredicates(
            cb,
            countRoot,
            criteria
        );

        // Apply predicates to queries
        if (!predicates.isEmpty()) {
            query.where(cb.and(predicates.toArray(new Predicate[0])));
            countQuery
                .select(cb.count(countRoot))
                .where(cb.and(countPredicates.toArray(new Predicate[0])));
        } else {
            countQuery.select(cb.count(countRoot));
        }

        // Make DISTINCT to avoid duplicates from joins
        query.distinct(true);
        countQuery.distinct(true);

        // Apply sorting
        query.orderBy(buildOrderBy(cb, movie, criteria));

        // Execute count query
        Long total = em.createQuery(countQuery).getSingleResult();

        // Execute main query with pagination
        List<Movie> movies = em
            .createQuery(query)
            .setFirstResult(criteria.page * criteria.pageSize)
            .setMaxResults(criteria.pageSize)
            .getResultList();

        return new MovieSearchResult(
            movies,
            total,
            criteria.page,
            criteria.pageSize
        );
    }

    /**
     * Build WHERE predicates safely using Criteria API
     */
    private List<Predicate> buildPredicates(
        CriteriaBuilder cb,
        Root<Movie> movie,
        MovieFilterCriteria criteria
    ) {
        List<Predicate> predicates = new ArrayList<>();

        // Filter by type (movie, series, episode)
        if (criteria.type != null && !criteria.type.isBlank()) {
            predicates.add(cb.equal(movie.get("type"), criteria.type));
        }

        // Filter by genres
        if (criteria.genreSlugs != null && !criteria.genreSlugs.isEmpty()) {
            Join<Object, Object> genreJoin = movie.join(
                "genres",
                JoinType.INNER
            );
            predicates.add(genreJoin.get("slug").in(criteria.genreSlugs));
        }

        // Filter by countries
        if (criteria.countryCodes != null && !criteria.countryCodes.isEmpty()) {
            Join<Object, Object> countryJoin = movie.join(
                "countries",
                JoinType.INNER
            );
            predicates.add(countryJoin.get("code").in(criteria.countryCodes));
        }

        // Filter by languages
        if (
            criteria.languageCodes != null && !criteria.languageCodes.isEmpty()
        ) {
            Join<Object, Object> languageJoin = movie.join(
                "languages",
                JoinType.INNER
            );
            predicates.add(languageJoin.get("code").in(criteria.languageCodes));
        }

        // Filter by studios
        if (criteria.studioNames != null && !criteria.studioNames.isEmpty()) {
            Join<Object, Object> studioJoin = movie.join(
                "studios",
                JoinType.INNER
            );
            predicates.add(studioJoin.get("name").in(criteria.studioNames));
        }

        // Filter by keywords
        if (criteria.keywordSlugs != null && !criteria.keywordSlugs.isEmpty()) {
            Join<Object, Object> keywordJoin = movie.join(
                "keywords",
                JoinType.INNER
            );
            predicates.add(keywordJoin.get("slug").in(criteria.keywordSlugs));
        }

        // Filter by year range
        if (criteria.yearFrom != null) {
            predicates.add(
                cb.greaterThanOrEqualTo(movie.get("year"), criteria.yearFrom)
            );
        }
        if (criteria.yearTo != null) {
            predicates.add(
                cb.lessThanOrEqualTo(movie.get("year"), criteria.yearTo)
            );
        }

        // Filter by IMDB rating
        if (criteria.minImdbRating != null) {
            predicates.add(
                cb.greaterThanOrEqualTo(
                    movie.get("imdbRating"),
                    criteria.minImdbRating
                )
            );
        }
        if (criteria.maxImdbRating != null) {
            predicates.add(
                cb.lessThanOrEqualTo(
                    movie.get("imdbRating"),
                    criteria.maxImdbRating
                )
            );
        }

        // Filter by runtime (duration)
        if (criteria.minRuntime != null) {
            predicates.add(
                cb.greaterThanOrEqualTo(
                    movie.get("runtime"),
                    criteria.minRuntime
                )
            );
        }
        if (criteria.maxRuntime != null) {
            predicates.add(
                cb.lessThanOrEqualTo(movie.get("runtime"), criteria.maxRuntime)
            );
        }

        // Filter by title (search)
        if (criteria.titleSearch != null && !criteria.titleSearch.isBlank()) {
            String searchPattern =
                "%" + criteria.titleSearch.toLowerCase() + "%";
            Predicate titleMatch = cb.like(
                cb.lower(movie.get("title")),
                searchPattern
            );
            Predicate originalTitleMatch = cb.like(
                cb.lower(movie.get("originalTitle")),
                searchPattern
            );
            predicates.add(cb.or(titleMatch, originalTitleMatch));
        }

        // Filter by director
        if (criteria.directorName != null && !criteria.directorName.isBlank()) {
            Join<Object, Object> creditJoin = movie.join(
                "credits",
                JoinType.INNER
            );
            Join<Object, Object> personJoin = creditJoin.join(
                "person",
                JoinType.INNER
            );
            predicates.add(cb.equal(creditJoin.get("roleType"), "director"));
            predicates.add(
                cb.like(
                    cb.lower(personJoin.get("name")),
                    "%" + criteria.directorName.toLowerCase() + "%"
                )
            );
        }

        // Filter by actor
        if (criteria.actorName != null && !criteria.actorName.isBlank()) {
            Join<Object, Object> creditJoin = movie.join(
                "credits",
                JoinType.INNER
            );
            Join<Object, Object> personJoin = creditJoin.join(
                "person",
                JoinType.INNER
            );
            predicates.add(cb.equal(creditJoin.get("roleType"), "actor"));
            predicates.add(
                cb.like(
                    cb.lower(personJoin.get("name")),
                    "%" + criteria.actorName.toLowerCase() + "%"
                )
            );
        }

        // Exclude nulls for rating-based sorting
        if (criteria.sortBy != null && criteria.sortBy.contains("rating")) {
            predicates.add(cb.isNotNull(movie.get("imdbRating")));
        }

        return predicates;
    }

    /**
     * Build ORDER BY clause safely
     */
    private List<Order> buildOrderBy(
        CriteriaBuilder cb,
        Root<Movie> movie,
        MovieFilterCriteria criteria
    ) {
        List<Order> orders = new ArrayList<>();

        String sortBy = criteria.sortBy != null ? criteria.sortBy : "rating";
        boolean ascending =
            criteria.sortAscending != null && criteria.sortAscending;

        switch (sortBy) {
            case "rating":
                orders.add(
                    ascending
                        ? cb.asc(movie.get("imdbRating"))
                        : cb.desc(movie.get("imdbRating"))
                );
                orders.add(cb.desc(movie.get("imdbVotes"))); // Secondary sort
                break;
            case "year":
                orders.add(
                    ascending
                        ? cb.asc(movie.get("year"))
                        : cb.desc(movie.get("year"))
                );
                break;
            case "title":
                orders.add(
                    ascending
                        ? cb.asc(movie.get("title"))
                        : cb.desc(movie.get("title"))
                );
                break;
            case "popularity":
                orders.add(cb.desc(movie.get("imdbVotes")));
                break;
            default:
                orders.add(cb.desc(movie.get("imdbRating")));
        }

        return orders;
    }

    /**
     * Find movie by IMDB ID (safe)
     */
    public Movie findByImdbId(String imdbId) {
        return find("imdbId", imdbId).firstResult();
    }

    /**
     * Quick search by title (safe)
     */
    public List<Movie> quickSearchByTitle(String title, int limit) {
        if (title == null || title.isBlank()) {
            return List.of();
        }

        Parameters params = Parameters.with(
            "pattern",
            "%" + title.toLowerCase() + "%"
        );

        return find(
            "LOWER(title) LIKE :pattern OR LOWER(originalTitle) LIKE :pattern",
            params
        )
            .page(0, limit)
            .list();
    }

    /**
     * Result wrapper with pagination info
     */
    public static class MovieSearchResult {

        public final List<Movie> movies;
        public final long total;
        public final int page;
        public final int pageSize;
        public final int totalPages;

        public MovieSearchResult(
            List<Movie> movies,
            long total,
            int page,
            int pageSize
        ) {
            this.movies = movies;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
            this.totalPages = (int) Math.ceil((double) total / pageSize);
        }
    }

    /**
     * Filter criteria object - all filters in one place
     */
    public static class MovieFilterCriteria {

        // Type filter
        public String type; // "movie", "series", "episode"

        // Multi-select filters
        public List<String> genreSlugs;
        public List<String> countryCodes;
        public List<String> languageCodes;
        public List<String> studioNames;
        public List<String> keywordSlugs;

        // Range filters
        public Integer yearFrom;
        public Integer yearTo;
        public BigDecimal minImdbRating;
        public BigDecimal maxImdbRating;
        public Integer minRuntime;
        public Integer maxRuntime;

        // Text search filters
        public String titleSearch;
        public String directorName;
        public String actorName;

        // Pagination
        public int page = 0;
        public int pageSize = 50;

        // Sorting
        public String sortBy = "rating"; // "rating", "year", "title", "popularity"
        public Boolean sortAscending = false;

        // Builder pattern for convenience
        public static MovieFilterCriteria builder() {
            return new MovieFilterCriteria();
        }

        public MovieFilterCriteria withType(String type) {
            this.type = type;
            return this;
        }

        public MovieFilterCriteria withGenres(List<String> genreSlugs) {
            this.genreSlugs = genreSlugs;
            return this;
        }

        public MovieFilterCriteria withCountries(List<String> countryCodes) {
            this.countryCodes = countryCodes;
            return this;
        }

        public MovieFilterCriteria withYearRange(Integer from, Integer to) {
            this.yearFrom = from;
            this.yearTo = to;
            return this;
        }

        public MovieFilterCriteria withMinRating(BigDecimal minRating) {
            this.minImdbRating = minRating;
            return this;
        }

        public MovieFilterCriteria withTitleSearch(String title) {
            this.titleSearch = title;
            return this;
        }

        public MovieFilterCriteria withPage(int page, int pageSize) {
            this.page = page;
            this.pageSize = pageSize;
            return this;
        }

        public MovieFilterCriteria withSorting(
            String sortBy,
            boolean ascending
        ) {
            this.sortBy = sortBy;
            this.sortAscending = ascending;
            return this;
        }
    }
}
