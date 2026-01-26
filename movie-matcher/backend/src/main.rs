use axum::{
    http::{header, Method},
    response::IntoResponse,
    routing::{get, post},
    Json, Router,
};
use dashmap::DashMap;
use std::sync::Arc;
use tower_http::cors::{Any, CorsLayer};
use tracing::info;

mod handlers;
mod models;
mod omdb;
mod room;

use handlers::{
    create_room, get_room, get_room_state, join_room, start_matching, update_filters, ws_handler,
};
use models::AppState;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    // Load environment variables
    dotenv::dotenv().ok();

    // Initialize tracing
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env().unwrap_or_else(|_| "info".into()),
        )
        .init();

    // Verify OMDB API key is set
    let api_key = std::env::var("OMDB_API_KEY").expect("OMDB_API_KEY must be set");
    info!("OMDB API key loaded successfully");

    // Create shared state
    let state = Arc::new(AppState {
        rooms: DashMap::new(),
        omdb_api_key: api_key,
    });

    // Configure CORS
    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods([Method::GET, Method::POST, Method::OPTIONS])
        .allow_headers([header::CONTENT_TYPE]);

    // Build router
    let app = Router::new()
        .route("/health", get(health_check))
        .route("/api/rooms", post(create_room))
        .route("/api/rooms/:room_id", get(get_room))
        .route("/api/rooms/:room_id/state", get(get_room_state))
        .route("/api/rooms/:room_id/join", post(join_room))
        .route(
            "/api/rooms/:room_id/filters",
            axum::routing::put(update_filters),
        )
        .route("/api/rooms/:room_id/start", post(start_matching))
        .route("/api/rooms/:room_id/ws", get(ws_handler))
        .layer(cors)
        .with_state(state);

    // Start server
    let addr = "0.0.0.0:3000";
    info!("Starting server on {}", addr);

    let listener = tokio::net::TcpListener::bind(addr).await?;
    axum::serve(listener, app).await?;

    Ok(())
}

async fn health_check() -> impl IntoResponse {
    Json(serde_json::json!({
        "status": "ok",
        "service": "movie-matcher-backend"
    }))
}
