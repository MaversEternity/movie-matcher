use crate::models::{MovieData, RoomFilters};
use anyhow::{anyhow, Result};
use futures::future::join_all;
use serde::Deserialize;
use tracing::{error, info};

#[derive(Debug, Deserialize)]
struct OmdbSearchResponse {
    #[serde(rename = "Search")]
    search: Option<Vec<OmdbSearchResult>>,
    #[serde(rename = "Response")]
    response: String,
    #[serde(rename = "Error")]
    _error: Option<String>,
}

#[derive(Debug, Deserialize)]
struct OmdbSearchResult {
    #[serde(rename = "Title")]
    title: String,
    #[serde(rename = "Year")]
    year: String,
    #[serde(rename = "imdbID")]
    imdb_id: String,
}

#[derive(Debug, Deserialize)]
struct OmdbDetailResponse {
    #[serde(rename = "Title")]
    title: String,
    #[serde(rename = "Year")]
    year: String,
    #[serde(rename = "Poster")]
    poster: String,
    #[serde(rename = "Plot")]
    plot: String,
    #[serde(rename = "Genre")]
    genre: String,
    #[serde(rename = "imdbRating")]
    imdb_rating: String,
    #[serde(rename = "imdbID")]
    imdb_id: String,
    #[serde(rename = "Response")]
    response: String,
}

pub async fn fetch_random_movies(
    api_key: &str,
    filters: &RoomFilters,
    count: usize,
) -> Result<Vec<MovieData>> {
    // Search terms that typically yield good results
    let search_terms = vec![
        "love",
        "war",
        "adventure",
        "mystery",
        "family",
        "hero",
        "night",
        "city",
        "world",
        "story",
        "life",
        "death",
        "time",
        "space",
        "dream",
        "power",
        "lost",
        "last",
    ];

    let client = reqwest::Client::new();
    let mut all_candidates = Vec::new();

    // Perform multiple searches in parallel
    let num_searches = (count / 3).max(3);
    let search_futures: Vec<_> = (0..num_searches)
        .map(|i| {
            let search_term = search_terms[i % search_terms.len()];
            let year = if let (Some(from), Some(to)) = (filters.year_from, filters.year_to) {
                Some(from + ((i as u32) % (to - from + 1)))
            } else {
                filters.year_from.or(filters.year_to)
            };

            let mut url = format!(
                "https://www.omdbapi.com/?apikey={}&s={}&type=movie",
                api_key, search_term
            );

            if let Some(y) = year {
                url.push_str(&format!("&y={}", y));
            }

            let client = client.clone();
            async move {
                match client.get(&url).send().await {
                    Ok(response) => {
                        if let Ok(search_result) = response.json::<OmdbSearchResponse>().await {
                            if search_result.response == "True" {
                                return search_result.search.unwrap_or_default();
                            }
                        }
                    }
                    Err(e) => {
                        error!("Error searching movies: {}", e);
                    }
                }
                Vec::new()
            }
        })
        .collect();

    // Wait for all searches to complete
    let search_results = join_all(search_futures).await;

    // Collect all candidates
    for results in search_results {
        all_candidates.extend(results);
    }

    // Remove duplicates
    all_candidates.sort_by(|a, b| a.imdb_id.cmp(&b.imdb_id));
    all_candidates.dedup_by(|a, b| a.imdb_id == b.imdb_id);

    // Limit candidates to avoid too many API calls
    all_candidates.truncate(count * 2);

    info!("Found {} candidate movies", all_candidates.len());

    // Fetch details for all candidates in parallel
    let detail_futures: Vec<_> = all_candidates
        .iter()
        .map(|movie| {
            let api_key = api_key.to_string();
            let imdb_id = movie.imdb_id.clone();
            async move { fetch_movie_details(&api_key, &imdb_id).await }
        })
        .collect();

    let detail_results = join_all(detail_futures).await;

    // Filter and collect movies
    let mut movies = Vec::new();
    for movie_result in detail_results {
        if let Ok(movie_data) = movie_result {
            // Check genre filter
            if let Some(genre_filter) = &filters.genre {
                if !movie_data
                    .genre
                    .to_lowercase()
                    .contains(&genre_filter.to_lowercase())
                {
                    continue;
                }
            }

            movies.push(movie_data);

            // Stop once we have enough movies
            if movies.len() >= count {
                break;
            }
        }
    }

    if movies.is_empty() {
        Err(anyhow!("Could not fetch any movies matching the filters"))
    } else {
        info!("Successfully fetched {} movies", movies.len());
        Ok(movies)
    }
}

async fn fetch_movie_details(api_key: &str, imdb_id: &str) -> Result<MovieData> {
    let client = reqwest::Client::new();
    let url = format!(
        "https://www.omdbapi.com/?apikey={}&i={}&plot=short",
        api_key, imdb_id
    );

    let response = client.get(&url).send().await?;
    let detail: OmdbDetailResponse = response.json().await?;

    if detail.response != "True" {
        return Err(anyhow!("Movie not found"));
    }

    Ok(MovieData {
        title: detail.title,
        year: detail.year,
        poster: detail.poster,
        plot: detail.plot,
        genre: detail.genre,
        imdb_rating: detail.imdb_rating,
        imdb_id: detail.imdb_id,
    })
}
