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
    error: Option<String>,
    #[serde(rename = "totalResults")]
    total_results: Option<String>,
}

#[derive(Debug, Deserialize)]
struct OmdbSearchResult {
    #[serde(rename = "imdbID")]
    imdb_id: String,
    #[serde(rename = "Type")]
    movie_type: String,
}

#[derive(Debug, Deserialize)]
struct OmdbDetailResponse {
    #[serde(rename = "Title")]
    title: String,
    #[serde(rename = "Year")]
    year: String,
    #[serde(rename = "Rated")]
    rated: String,
    #[serde(rename = "Runtime")]
    runtime: String,
    #[serde(rename = "Genre")]
    genre: String,
    #[serde(rename = "Director")]
    director: String,
    #[serde(rename = "Actors")]
    actors: String,
    #[serde(rename = "Plot")]
    plot: String,
    #[serde(rename = "Country")]
    country: String,
    #[serde(rename = "Poster")]
    poster: String,
    #[serde(rename = "imdbRating")]
    imdb_rating: String,
    #[serde(rename = "imdbID")]
    imdb_id: String,
    #[serde(rename = "Response")]
    response: String,
}

pub async fn fetch_movies_by_page(
    api_key: &str,
    filters: &RoomFilters,
    page: u32,
) -> Result<(Vec<MovieData>, bool)> {
    let client = reqwest::Client::new();
    let search_term = filters.genre.as_deref().unwrap_or("movie");
    let mut url = format!(
        "https://www.omdbapi.com/?apikey={}&s={}&type=movie&page={}",
        api_key, search_term, page
    );
    if let Some(year_from) = filters.year_from {
        url.push_str(&format!("&y={}", year_from));
    }

    let response = client.get(&url).send().await?;
    let search_result: OmdbSearchResponse = response.json().await?;

    if search_result.response != "True" {
        return Err(anyhow!("OMDB error: {}", search_result.error.unwrap_or_default()));
    }

    let search_results = search_result.search.unwrap_or_default();
    let total: u32 = search_result.total_results.and_then(|s| s.parse().ok()).unwrap_or(0);
    let has_more = (page * 10) < total;

    // Fetch details in parallel
    let detail_futures: Vec<_> = search_results
        .iter()
        .filter(|r| r.movie_type == "movie")
        .map(|r| fetch_movie_details(api_key, &r.imdb_id))
        .collect();

    let results = join_all(detail_futures).await;
    let mut movies: Vec<MovieData> = results.into_iter().filter_map(|r| r.ok()).collect();

    // Apply year range filter
    if let (Some(from), Some(to)) = (filters.year_from, filters.year_to) {
        movies.retain(|m| {
            m.year.split('–').next()
                .and_then(|y| y.parse::<u32>().ok())
                .map(|y| y >= from && y <= to)
                .unwrap_or(true)
        });
    }

    info!("Fetched {} movies from page {}", movies.len(), page);
    if movies.is_empty() {
        Err(anyhow!("No movies found"))
    } else {
        Ok((movies, has_more))
    }
}

async fn fetch_movie_details(api_key: &str, imdb_id: &str) -> Result<MovieData> {
    let url = format!("https://www.omdbapi.com/?apikey={}&i={}&plot=short", api_key, imdb_id);
    let response = reqwest::get(&url).await?;
    let detail: OmdbDetailResponse = response.json().await?;

    if detail.response != "True" {
        return Err(anyhow!("Movie not found"));
    }

    Ok(MovieData {
        title: detail.title,
        year: detail.year,
        rated: if detail.rated == "N/A" { String::new() } else { detail.rated },
        runtime: if detail.runtime == "N/A" { String::new() } else { detail.runtime },
        poster: if detail.poster == "N/A" { String::new() } else { detail.poster },
        director: if detail.director == "N/A" { String::new() } else { detail.director },
        actors: if detail.actors == "N/A" { String::new() } else { detail.actors },
        plot: if detail.plot == "N/A" { String::new() } else { detail.plot },
        country: if detail.country == "N/A" { String::new() } else { detail.country },
        genre: detail.genre,
        imdb_rating: if detail.imdb_rating == "N/A" { String::new() } else { detail.imdb_rating },
        imdb_id: detail.imdb_id,
    })
}
