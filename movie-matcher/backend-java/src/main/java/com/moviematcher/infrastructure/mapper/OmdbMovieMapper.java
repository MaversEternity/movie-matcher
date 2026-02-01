package com.moviematcher.infrastructure.mapper;

import com.moviematcher.client.OmdbDetailResponse;
import com.moviematcher.entity.Movie;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.mapstruct.*;

/**
 * MapStruct маппер для OMDB API
 *
 * OMDB обычно возвращает данные на английском,
 * но мы будем использовать его как fallback если TMDB не нашел
 */
@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OmdbMovieMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "imdbId", source = "imdbId")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "originalTitle", source = "title")
    @Mapping(target = "plot", source = "plot", qualifiedByName = "handleNA")
    @Mapping(
        target = "plotShort",
        source = "plot",
        qualifiedByName = "handleNA"
    )
    @Mapping(target = "type", source = "type", qualifiedByName = "mapType")
    @Mapping(target = "year", source = "year", qualifiedByName = "parseYear")
    @Mapping(
        target = "releaseDate",
        source = "released",
        qualifiedByName = "parseReleaseDate"
    )
    @Mapping(
        target = "runtime",
        source = "runtime",
        qualifiedByName = "parseRuntime"
    )
    @Mapping(
        target = "posterUrl",
        source = "poster",
        qualifiedByName = "handleNA"
    )
    @Mapping(
        target = "imdbRating",
        source = "imdbRating",
        qualifiedByName = "parseRating"
    )
    @Mapping(
        target = "imdbVotes",
        source = "imdbVotes",
        qualifiedByName = "parseVotes"
    )
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Movie toMovie(OmdbDetailResponse response);

    // ============ Custom Mapping Methods ============

    @Named("handleNA")
    default String handleNA(String value) {
        return "N/A".equals(value) ? null : value;
    }

    @Named("mapType")
    default String mapType(String omdbType) {
        if (omdbType == null) return "movie";
        return switch (omdbType.toLowerCase()) {
            case "movie" -> "movie";
            case "series" -> "series";
            case "episode" -> "episode";
            default -> "movie";
        };
    }

    @Named("parseYear")
    default Integer parseYear(String year) {
        if (year == null || "N/A".equals(year)) return null;
        try {
            // Формат может быть "2010" или "2010-2015" для сериалов
            String yearStr = year.split("–")[0].split("-")[0].trim();
            return Integer.parseInt(yearStr);
        } catch (Exception e) {
            return null;
        }
    }

    @Named("parseReleaseDate")
    default LocalDate parseReleaseDate(String released) {
        if (released == null || "N/A".equals(released)) return null;
        try {
            // OMDB формат: "14 Oct 1994"
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                "dd MMM yyyy",
                Locale.ENGLISH
            );
            return LocalDate.parse(released, formatter);
        } catch (Exception e) {
            return null;
        }
    }

    @Named("parseRuntime")
    default Integer parseRuntime(String runtime) {
        if (runtime == null || "N/A".equals(runtime)) return null;
        try {
            // "142 min" -> 142
            return Integer.parseInt(runtime.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return null;
        }
    }

    @Named("parseRating")
    default BigDecimal parseRating(String rating) {
        if (rating == null || "N/A".equals(rating)) return null;
        try {
            return new BigDecimal(rating);
        } catch (Exception e) {
            return null;
        }
    }

    @Named("parseVotes")
    default Integer parseVotes(String votes) {
        if (votes == null || "N/A".equals(votes)) return null;
        try {
            // "1,234,567" -> 1234567
            return Integer.parseInt(votes.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return null;
        }
    }
}
