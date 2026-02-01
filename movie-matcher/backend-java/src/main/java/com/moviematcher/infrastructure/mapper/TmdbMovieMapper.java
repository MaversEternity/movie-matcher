package com.moviematcher.infrastructure.mapper;

import com.moviematcher.client.tmdb.*;
import com.moviematcher.entity.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.*;

/**
 * MapStruct маппер для конвертации TMDB API ответов в Movie entity
 *
 * MapStruct генерирует реализацию во время компиляции
 * Преимущества:
 * - Type-safe маппинг
 * - Высокая производительность (нет рефлексии)
 * - Явная конфигурация маппинга
 *
 * ВАЖНО: TMDB возвращает данные на русском языке при language=ru-RU!
 */
@Mapper(
    componentModel = "cdi",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface TmdbMovieMapper {
    String TMDB_IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";
    String TMDB_BACKDROP_BASE_URL = "https://image.tmdb.org/t/p/original";

    /**
     * Конвертация полного TMDB ответа в Movie entity
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imdbId", source = "response.imdbId")
    @Mapping(target = "title", source = "response.title") // Русское название от TMDB!
    @Mapping(target = "originalTitle", source = "response.originalTitle")
    @Mapping(target = "plot", source = "response.overview") // Русское описание!
    @Mapping(
        target = "plotShort",
        source = "response.overview",
        qualifiedByName = "truncateOverview"
    )
    @Mapping(target = "type", constant = "movie")
    @Mapping(
        target = "releaseDate",
        source = "response.releaseDate",
        qualifiedByName = "parseDate"
    )
    @Mapping(
        target = "year",
        source = "response.releaseDate",
        qualifiedByName = "extractYear"
    )
    @Mapping(target = "runtime", source = "response.runtime")
    @Mapping(
        target = "posterUrl",
        source = "response.posterPath",
        qualifiedByName = "buildPosterUrl"
    )
    @Mapping(
        target = "backdropUrl",
        source = "response.backdropPath",
        qualifiedByName = "buildBackdropUrl"
    )
    @Mapping(
        target = "imdbRating",
        source = "response.voteAverage",
        qualifiedByName = "toBigDecimal"
    )
    @Mapping(target = "imdbVotes", source = "response.voteCount")
    @Mapping(target = "budget", source = "response.budget")
    @Mapping(target = "boxOffice", source = "response.revenue")
    @Mapping(
        target = "genres",
        source = "response.genres",
        qualifiedByName = "mapGenres"
    )
    @Mapping(
        target = "countries",
        source = "response.productionCountries",
        qualifiedByName = "mapCountries"
    )
    @Mapping(
        target = "languages",
        source = "response.spokenLanguages",
        qualifiedByName = "mapLanguages"
    )
    @Mapping(
        target = "studios",
        source = "response.productionCompanies",
        qualifiedByName = "mapStudios"
    )
    @Mapping(
        target = "credits",
        source = "credits",
        qualifiedByName = "mapCredits"
    )
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "metacriticScore", ignore = true)
    @Mapping(target = "rottenTomatoesScore", ignore = true)
    @Mapping(target = "awards", ignore = true)
    @Mapping(target = "keywords", ignore = true)
    Movie toMovie(TmdbMovieResponse response, TmdbCreditsResponse credits);

    /**
     * Конвертация поискового результата
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "title", source = "title")
    @Mapping(target = "originalTitle", source = "originalTitle")
    @Mapping(target = "plot", source = "overview")
    @Mapping(
        target = "plotShort",
        source = "overview",
        qualifiedByName = "truncateOverview"
    )
    @Mapping(target = "type", constant = "movie")
    @Mapping(
        target = "releaseDate",
        source = "releaseDate",
        qualifiedByName = "parseDate"
    )
    @Mapping(
        target = "year",
        source = "releaseDate",
        qualifiedByName = "extractYear"
    )
    @Mapping(
        target = "posterUrl",
        source = "posterPath",
        qualifiedByName = "buildPosterUrl"
    )
    @Mapping(
        target = "backdropUrl",
        source = "backdropPath",
        qualifiedByName = "buildBackdropUrl"
    )
    @Mapping(
        target = "imdbRating",
        source = "voteAverage",
        qualifiedByName = "toBigDecimal"
    )
    @Mapping(target = "imdbVotes", source = "voteCount")
    Movie toMovieFromSearchResult(TmdbSearchResult result);

    // ============ Custom Mapping Methods ============

    @Named("parseDate")
    default LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    @Named("extractYear")
    default Integer extractYear(String dateStr) {
        LocalDate date = parseDate(dateStr);
        return date != null ? date.getYear() : null;
    }

    @Named("truncateOverview")
    default String truncateOverview(String overview) {
        if (overview == null) return null;
        return overview.length() > 1000
            ? overview.substring(0, 1000)
            : overview;
    }

    @Named("buildPosterUrl")
    default String buildPosterUrl(String posterPath) {
        return posterPath != null ? TMDB_IMAGE_BASE_URL + posterPath : null;
    }

    @Named("buildBackdropUrl")
    default String buildBackdropUrl(String backdropPath) {
        return backdropPath != null
            ? TMDB_BACKDROP_BASE_URL + backdropPath
            : null;
    }

    @Named("toBigDecimal")
    default BigDecimal toBigDecimal(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }

    @Named("mapGenres")
    default Set<Genre> mapGenres(List<TmdbGenre> tmdbGenres) {
        if (tmdbGenres == null) return Set.of();

        return tmdbGenres
            .stream()
            .map(tg -> {
                Genre genre = Genre.find("name", tg.name()).firstResult();
                if (genre == null) {
                    genre = new Genre();
                    genre.name = tg.name();
                    genre.slug = slugify(tg.name());
                }
                return genre;
            })
            .collect(Collectors.toSet());
    }

    @Named("mapCountries")
    default Set<Country> mapCountries(List<TmdbCountry> tmdbCountries) {
        if (tmdbCountries == null) return Set.of();

        return tmdbCountries
            .stream()
            .map(tc -> {
                Country country = Country.find(
                    "code",
                    tc.iso31661()
                ).firstResult();
                if (country == null) {
                    country = new Country();
                    country.code = tc.iso31661();
                    country.name = tc.name();
                }
                return country;
            })
            .collect(Collectors.toSet());
    }

    @Named("mapLanguages")
    default Set<Language> mapLanguages(List<TmdbLanguage> tmdbLanguages) {
        if (tmdbLanguages == null) return Set.of();

        return tmdbLanguages
            .stream()
            .map(tl -> {
                Language language = Language.find(
                    "code",
                    tl.iso6391()
                ).firstResult();
                if (language == null) {
                    language = new Language();
                    language.code = tl.iso6391();
                    language.name = tl.name();
                }
                return language;
            })
            .collect(Collectors.toSet());
    }

    @Named("mapStudios")
    default Set<Studio> mapStudios(List<TmdbCompany> tmdbCompanies) {
        if (tmdbCompanies == null) return Set.of();

        return tmdbCompanies
            .stream()
            .map(tc -> {
                Studio studio = Studio.find("name", tc.name()).firstResult();
                if (studio == null) {
                    studio = new Studio();
                    studio.name = tc.name();
                }
                return studio;
            })
            .collect(Collectors.toSet());
    }

    @Named("mapCredits")
    default Set<MovieCredit> mapCredits(TmdbCreditsResponse credits) {
        if (credits == null) return Set.of();

        Set<MovieCredit> movieCredits = new java.util.HashSet<>();

        // Актеры (первые 10)
        if (credits.cast() != null) {
            credits
                .cast()
                .stream()
                .limit(10)
                .forEach(cast -> {
                    Person person = findOrCreatePerson(cast.name());
                    MovieCredit credit = new MovieCredit();
                    credit.person = person;
                    credit.roleType = "actor";
                    credit.characterName = cast.character();
                    credit.orderIndex = cast.order();
                    movieCredits.add(credit);
                });
        }

        // Режиссер
        if (credits.crew() != null) {
            credits
                .crew()
                .stream()
                .filter(crew -> "Director".equals(crew.job()))
                .findFirst()
                .ifPresent(director -> {
                    Person person = findOrCreatePerson(director.name());
                    MovieCredit credit = new MovieCredit();
                    credit.person = person;
                    credit.roleType = "director";
                    movieCredits.add(credit);
                });
        }

        return movieCredits;
    }

    // ============ Helper Methods ============

    default Person findOrCreatePerson(String name) {
        Person person = Person.find("name", name).firstResult();
        if (person == null) {
            person = new Person();
            person.name = name;
        }
        return person;
    }

    default String slugify(String text) {
        if (text == null) return "";
        return text
            .toLowerCase()
            .replaceAll("[^a-z0-9а-яё\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }
}
