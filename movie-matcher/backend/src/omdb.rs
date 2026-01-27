use crate::models::{MovieData, RoomFilters};
use anyhow::{anyhow, Result};
use serde::Deserialize;
use tracing::{error, info};

#[derive(Debug, Deserialize)]
struct OmdbSearchResponse {
    #[serde(rename = "Search")]
    search: Option<Vec<OmdbMovie>>,
    #[serde(rename = "Response")]
    response: String,
    #[serde(rename = "Error")]
    error: Option<String>,
    #[serde(rename = "totalResults")]
    total_results: Option<String>,
}

#[derive(Debug, Deserialize)]
struct OmdbMovie {
    #[serde(rename = "Title")]
    title: String,
    #[serde(rename = "Year")]
    year: String,
    #[serde(rename = "imdbID")]
    imdb_id: String,
    #[serde(rename = "Type")]
    movie_type: String,
    #[serde(rename = "Poster")]
    poster: String,
}

pub async fn fetch_movies_by_page(
    api_key: &str,
    filters: &RoomFilters,
    page: u32,
) -> Result<(Vec<MovieData>, bool)> {
    let client = reqwest::Client::new();
    
    // Build search query based on genre filter
    let search_term = if let Some(genre) = &filters.genre {
        genre.clone()
    } else {
        "movie".to_string()
    };
    
    let mut url = format!(
        "https://www.omdbapi.com/?apikey={}&s={}&type=movie&page={}",
        api_key, search_term, page
    );
    
    // Add year filter if specified
    if let Some(year_from) = filters.year_from {
        if let Some(year_to) = filters.year_to {
            // OMDB doesn't support year ranges, so we'll use year_from as primary filter
            url.push_str(&format!("&y={}", year_from));
        } else {
            url.push_str(&format!("&y={}", year_from));
        }
    } else if let Some(year_to) = filters.year_to {
        url.push_str(&format!("&y={}", year_to));
    }
    
    info!("Fetching movies from OMDB: {}", url);
    
    let response = client.get(&url).send().await?;
    let search_result: OmdbSearchResponse = response.json().await?;
    
    if search_result.response != "True" {
        let err_msg = search_result.error.unwrap_or_else(|| "Unknown error".to_string());
        error!("OMDB API error: {}", err_msg);
        return Err(anyhow!("OMDB API error: {}", err_msg));
    }
    
    let movies_from_api = search_result.search.unwrap_or_default();
    
    // Parse total results to determine if there are more pages
    let total_results: u32 = search_result
        .total_results
        .and_then(|s| s.parse().ok())
        .unwrap_or(0);
    
    let has_more_pages = (page * 10) < total_results;
    
    // Convert to our MovieData format
    let movies: Vec<MovieData> = movies_from_api
        .into_iter()
        .filter(|m| m.movie_type == "movie") // Ensure it's a movie, not series
        .filter(|m| {
            // Apply year range filter client-side since OMDB doesn't support ranges
            if let (Some(year_from), Some(year_to)) = (filters.year_from, filters.year_to) {
                if let Ok(movie_year) = m.year.split('–').next().unwrap_or(&m.year).parse::<u32>() {
                    return movie_year >= year_from && movie_year <= year_to;
                }
            }
            true
        })
        .map(|m| MovieData {
            title: m.title,
            year: m.year,
            poster: if m.poster == "N/A" { String::new() } else { m.poster },
            plot: String::new(), // Not available in search results
            genre: filters.genre.clone().unwrap_or_else(|| "Unknown".to_string()),
            imdb_rating: String::new(), // Not available in search results
            imdb_id: m.imdb_id,
        })
        .collect();
    
    info!("Fetched {} movies from page {}, has_more: {}", movies.len(), page, has_more_pages);
    
    if movies.is_empty() {
        Err(anyhow!("No movies found"))
    } else {
        Ok((movies, has_more_pages))
    }
}
