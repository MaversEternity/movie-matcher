# Movie Repository Usage Examples

## Безопасный поиск фильмов с фильтрами

### Пример 1: Найти экшен-фильмы 2020-2024 с рейтингом выше 7.0

```java
@Inject
MovieRepository movieRepository;

public void findActionMovies() {
    var criteria = MovieRepository.MovieFilterCriteria.builder()
        .withType("movie")
        .withGenres(List.of("action"))
        .withYearRange(2020, 2024)
        .withMinRating(new BigDecimal("7.0"))
        .withPage(0, 20)
        .withSorting("rating", false);
    
    var result = movieRepository.findByFilters(criteria);
    
    System.out.println("Found " + result.total + " movies");
    System.out.println("Page " + (result.page + 1) + " of " + result.totalPages);
    
    for (Movie movie : result.movies) {
        System.out.println(movie.title + " (" + movie.year + ") - " + movie.imdbRating);
    }
}
```

### Пример 2: Найти французские драмы с определенными актерами

```java
public void findFrenchDramas() {
    var criteria = MovieRepository.MovieFilterCriteria.builder()
        .withType("movie")
        .withGenres(List.of("drama"))
        .withCountries(List.of("FR"))
        .withActorName("Marion Cotillard")
        .withMinRating(new BigDecimal("6.5"))
        .withPage(0, 50);
    
    var result = movieRepository.findByFilters(criteria);
    
    result.movies.forEach(movie -> {
        System.out.println(movie.title + " - " + movie.year);
    });
}
```

### Пример 3: Комбинированный поиск с множественными фильтрами

```java
public void advancedSearch() {
    var criteria = new MovieRepository.MovieFilterCriteria();
    
    // Тип контента
    criteria.type = "movie";
    
    // Жанры (можно несколько)
    criteria.genreSlugs = List.of("science-fiction", "action");
    
    // Страны производства
    criteria.countryCodes = List.of("US", "GB");
    
    // Языки
    criteria.languageCodes = List.of("en");
    
    // Годы выпуска
    criteria.yearFrom = 2015;
    criteria.yearTo = 2024;
    
    // Рейтинг IMDB
    criteria.minImdbRating = new BigDecimal("7.5");
    criteria.maxImdbRating = new BigDecimal("9.0");
    
    // Продолжительность
    criteria.minRuntime = 90;  // минимум 90 минут
    criteria.maxRuntime = 180; // максимум 3 часа
    
    // Поиск в названии
    criteria.titleSearch = "Matrix";
    
    // Режиссер
    criteria.directorName = "Christopher Nolan";
    
    // Пагинация
    criteria.page = 0;
    criteria.pageSize = 25;
    
    // Сортировка
    criteria.sortBy = "rating"; // "rating", "year", "title", "popularity"
    criteria.sortAscending = false;
    
    var result = movieRepository.findByFilters(criteria);
    
    System.out.println("Total found: " + result.total);
    System.out.println("Current page: " + result.page);
    System.out.println("Total pages: " + result.totalPages);
    
    result.movies.forEach(movie -> {
        System.out.printf("%s (%d) - Rating: %s%n", 
            movie.title, movie.year, movie.imdbRating);
    });
}
```

### Пример 4: Поиск сериалов с ключевыми словами

```java
public void findSeriesWithKeywords() {
    var criteria = MovieRepository.MovieFilterCriteria.builder()
        .withType("series")
        .withGenres(List.of("crime", "drama"))
        .withMinRating(new BigDecimal("8.0"))
        .withSorting("popularity", false)
        .withPage(0, 10);
    
    // Если нужны ключевые слова
    criteria.keywordSlugs = List.of("detective", "police");
    
    var result = movieRepository.findByFilters(criteria);
    
    result.movies.forEach(series -> {
        System.out.println(series.title + " - Votes: " + series.imdbVotes);
    });
}
```

### Пример 5: Быстрый поиск по названию

```java
public void quickTitleSearch() {
    List<Movie> movies = movieRepository.quickSearchByTitle("Avengers", 10);
    
    movies.forEach(movie -> {
        System.out.println(movie.title + " (" + movie.year + ")");
    });
}
```

### Пример 6: Найти фильм по IMDB ID

```java
public void findByImdbId() {
    Movie movie = movieRepository.findByImdbId("tt0111161");
    
    if (movie != null) {
        System.out.println("Found: " + movie.title);
        System.out.println("Year: " + movie.year);
        System.out.println("Rating: " + movie.imdbRating);
        
        // Жанры
        movie.genres.forEach(genre -> System.out.println("Genre: " + genre.name));
        
        // Страны
        movie.countries.forEach(country -> System.out.println("Country: " + country.name));
    }
}
```

## Безопасность

### Защита от SQL-инъекций

Все методы используют:
1. **JPA Criteria API** - параметризованные запросы
2. **Named Parameters** - безопасная передача параметров
3. **Type-safe операции** - проверка типов на уровне компиляции

❌ **НЕ безопасно** (SQL-инъекция возможна):
```java
String sql = "SELECT * FROM movies WHERE title = '" + userInput + "'";
```

✅ **Безопасно** (наш подход):
```java
cb.like(cb.lower(movie.get("title")), "%" + userInput.toLowerCase() + "%")
```

### Валидация входных данных

```java
public MovieSearchResult safeSearch(String userInput) {
    // Валидация
    if (userInput == null || userInput.length() > 200) {
        throw new IllegalArgumentException("Invalid search query");
    }
    
    var criteria = MovieRepository.MovieFilterCriteria.builder()
        .withTitleSearch(userInput)  // Безопасно - используется параметризованный запрос
        .withPage(0, 20);
    
    return movieRepository.findByFilters(criteria);
}
```

## REST API Example

```java
@Path("/api/movies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MovieResource {

    @Inject
    MovieRepository movieRepository;

    @POST
    @Path("/search")
    public MovieRepository.MovieSearchResult searchMovies(MovieSearchRequest request) {
        var criteria = MovieRepository.MovieFilterCriteria.builder()
            .withType(request.type)
            .withGenres(request.genres)
            .withCountries(request.countries)
            .withYearRange(request.yearFrom, request.yearTo)
            .withMinRating(request.minRating)
            .withTitleSearch(request.titleSearch)
            .withPage(request.page != null ? request.page : 0, 
                     request.pageSize != null ? request.pageSize : 50)
            .withSorting(request.sortBy, request.sortAscending);
        
        return movieRepository.findByFilters(criteria);
    }
}
```

## Performance Tips

1. **Используйте пагинацию** - всегда указывайте page и pageSize
2. **Ограничивайте жанры/страны** - не запрашивайте слишком много фильтров одновременно
3. **Индексы** - все ключевые поля проиндексированы в миграциях
4. **DISTINCT** - автоматически применяется при JOIN'ах для избежания дубликатов
