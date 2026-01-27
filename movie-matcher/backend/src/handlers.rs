use axum::{
    extract::{
        ws::{Message, WebSocket},
        Path, State, WebSocketUpgrade,
    },
    http::StatusCode,
    response::Response,
    Json,
};
use futures::{SinkExt, StreamExt};
use std::sync::Arc;
use tracing::{error, info, warn};

use crate::{
    models::{
        AppState, ClientMessage, CreateRoomRequest, CreateRoomResponse, JoinRoomRequest,
        JoinRoomResponse, MovieData, Room, RoomInfo, RoomState,
    },
    omdb,
};

pub async fn create_room(
    State(state): State<Arc<AppState>>,
    Json(req): Json<CreateRoomRequest>,
) -> Result<Json<CreateRoomResponse>, StatusCode> {
    let room = Room::new(req.filters, req.host_id);
    let room_id = room.id.clone();

    state.rooms.insert(room_id.clone(), room);

    info!("Created room: {}", room_id);

    Ok(Json(CreateRoomResponse {
        room_id: room_id.clone(),
        join_url: format!("/room/{}", room_id),
    }))
}

pub async fn get_room(
    State(state): State<Arc<AppState>>,
    Path(room_id): Path<String>,
) -> Result<Json<RoomInfo>, StatusCode> {
    let room = state.rooms.get(&room_id).ok_or(StatusCode::NOT_FOUND)?;

    Ok(Json(RoomInfo {
        id: room.id.clone(),
        filters: room.filters.clone(),
        participants_count: room.participants.len(),
        is_active: room.is_active,
    }))
}

pub async fn get_room_state(
    State(state): State<Arc<AppState>>,
    Path(room_id): Path<String>,
) -> Result<Json<RoomState>, StatusCode> {
    let room = state.rooms.get(&room_id).ok_or(StatusCode::NOT_FOUND)?;

    // Empty movie list for getting likes (we don't cache all movies on backend)
    let all_movies: Vec<MovieData> = Vec::new();

    Ok(Json(RoomState {
        id: room.id.clone(),
        filters: room.filters.clone(),
        participants: room.participants.clone(),
        is_active: room.is_active,
        host_id: room.host_id.clone(),
        all_likes: room.get_all_participant_likes(&all_movies),
        common_likes: room.get_common_likes(&all_movies),
    }))
}

pub async fn join_room(
    State(state): State<Arc<AppState>>,
    Path(room_id): Path<String>,
    Json(req): Json<JoinRoomRequest>,
) -> Result<Json<JoinRoomResponse>, StatusCode> {
    let mut room = state.rooms.get_mut(&room_id).ok_or(StatusCode::NOT_FOUND)?;

    let success = room.add_participant(req.participant_id.clone());

    if success {
        info!("Participant {} joined room {}", req.participant_id, room_id);
    }

    Ok(Json(JoinRoomResponse {
        success,
        room: Some(RoomInfo {
            id: room.id.clone(),
            filters: room.filters.clone(),
            participants_count: room.participants.len(),
            is_active: room.is_active,
        }),
    }))
}

pub async fn start_matching(
    State(state): State<Arc<AppState>>,
    Path(room_id): Path<String>,
) -> Result<StatusCode, StatusCode> {
    let mut room = state.rooms.get_mut(&room_id).ok_or(StatusCode::NOT_FOUND)?;

    room.start();

    // Spawn background task for infinite movie streaming
    let state_clone = state.clone();
    let room_id_clone = room_id.clone();
    tokio::spawn(async move {
        if let Err(e) = stream_movies_infinitely(state_clone, room_id_clone).await {
            error!("Error streaming movies: {}", e);
        }
    });

    Ok(StatusCode::OK)
}

pub async fn update_filters(
    State(state): State<Arc<AppState>>,
    Path(room_id): Path<String>,
    Json(filters): Json<crate::models::RoomFilters>,
) -> Result<StatusCode, StatusCode> {
    let mut room = state.rooms.get_mut(&room_id).ok_or(StatusCode::NOT_FOUND)?;
    room.filters = filters.clone();
    info!("Updated filters for room {}: {:?}", room_id, filters);
    Ok(StatusCode::OK)
}

async fn stream_movies_infinitely(state: Arc<AppState>, room_id: String) -> anyhow::Result<()> {
    // Get room filters
    let filters = {
        let room = state
            .rooms
            .get(&room_id)
            .ok_or(anyhow::anyhow!("Room not found"))?;
        room.filters.clone()
    };

    // Stream movies page by page until room becomes inactive or we run out
    loop {
        // Check if room still exists and is active
        let current_page = {
            let room = state.rooms.get(&room_id);
            if room.is_none() || !room.as_ref().unwrap().is_active {
                info!(
                    "Room {} is no longer active, stopping movie stream",
                    room_id
                );
                break;
            }
            room.unwrap().current_page
        };

        // Fetch a page of movies
        match omdb::fetch_movies_by_page(&state.omdb_api_key, &filters, current_page).await {
            Ok((movies, has_more_pages)) => {
                // Filter out duplicates using room's sent_movie_ids
                let new_movies: Vec<MovieData> = {
                    let mut room = state
                        .rooms
                        .get_mut(&room_id)
                        .ok_or(anyhow::anyhow!("Room not found"))?;

                    let mut filtered = Vec::new();
                    for movie in movies {
                        if !room.sent_movie_ids.contains(&movie.imdb_id) {
                            room.sent_movie_ids.insert(movie.imdb_id.clone());
                            filtered.push(movie);
                        }
                    }

                    // Increment page for next fetch
                    room.current_page += 1;

                    filtered
                };

                if new_movies.is_empty() && !has_more_pages {
                    info!(
                        "Exhausted all pages for room {}, stopping stream (room stays active)",
                        room_id
                    );
                    
                    // Stop streaming - users can still finish their queue
                    if let Some(mut room) = state.rooms.get_mut(&room_id) {
                        if let Some(tx) = &room.tx {
                            let _ = tx.send(crate::models::ServerMessage::StreamingEnded);
                        }
                        let all_movies: Vec<MovieData> = Vec::new();
                        // Just stop streaming, dont end matching
                    }
                    break;
                }

                if new_movies.is_empty() {
                    // No new movies on this page, but more pages available - continue to next page
                    info!("Page {} had no new movies, trying next page", current_page);
                    tokio::time::sleep(tokio::time::Duration::from_millis(500)).await;
                    continue;
                }

                info!(
                    "Fetched {} new movies from page {} for room {}, has_more: {}",
                    new_movies.len(),
                    current_page,
                    room_id,
                    has_more_pages
                );

                // Broadcast each movie
                for movie in new_movies {
                    // Check again if room is still active
                    {
                        let room = state.rooms.get(&room_id);
                        if room.is_none() || !room.as_ref().unwrap().is_active {
                            break;
                        }
                    }

                    if let Some(room) = state.rooms.get(&room_id) {
                        room.broadcast_movie(movie);
                    }

                    // Small delay between movies
                    tokio::time::sleep(tokio::time::Duration::from_millis(200)).await;
                }

                // If no more pages, stop streaming
                if !has_more_pages {
                    info!("No more pages available for room {}, stopping stream (room stays active)", room_id);
                    
                    if let Some(mut room) = state.rooms.get_mut(&room_id) {
                        if let Some(tx) = &room.tx {
                            let _ = tx.send(crate::models::ServerMessage::StreamingEnded);
                        }
                        let all_movies: Vec<MovieData> = Vec::new();
                        // Just stop streaming, dont end matching
                        // Users can still finish movies in their queue
                    }
                    break;
                }
            }
            Err(e) => {
                error!(
                    "Error fetching movies for room {} on page {}: {}",
                    room_id, current_page, e
                );
                
                // End matching on API error
                if let Some(mut room) = state.rooms.get_mut(&room_id) {
                    let all_movies: Vec<MovieData> = Vec::new();
                    room.end_matching(&all_movies);
                }
                break;
            }
        }

        // Small delay before fetching next page
        tokio::time::sleep(tokio::time::Duration::from_secs(1)).await;
    }

    Ok(())
}
pub async fn ws_handler(
    ws: WebSocketUpgrade,
    State(state): State<Arc<AppState>>,
    Path(room_id): Path<String>,
) -> Response {
    ws.on_upgrade(move |socket| handle_socket(socket, state, room_id))
}

async fn handle_socket(socket: WebSocket, state: Arc<AppState>, room_id: String) {
    let (mut sender, mut receiver) = socket.split();

    // Subscribe to room broadcasts
    let mut rx = {
        let room = match state.rooms.get(&room_id) {
            Some(r) => r,
            None => {
                error!("Room {} not found for WebSocket connection", room_id);
                return;
            }
        };

        match &room.tx {
            Some(tx) => tx.subscribe(),
            None => {
                error!("Room {} has no broadcast channel", room_id);
                return;
            }
        }
    };

    // Clone state for handling client messages
    let state_for_recv = state.clone();
    let room_id_for_recv = room_id.clone();

    // Spawn task to forward broadcasts to WebSocket
    let mut send_task = tokio::spawn(async move {
        while let Ok(msg) = rx.recv().await {
            let json = match serde_json::to_string(&msg) {
                Ok(j) => j,
                Err(e) => {
                    error!("Error serializing message: {}", e);
                    continue;
                }
            };

            if sender.send(Message::Text(json)).await.is_err() {
                break;
            }
        }
    });

    // Handle incoming messages from client
    let mut recv_task = tokio::spawn(async move {
        let mut all_movies: Vec<MovieData> = Vec::new();

        while let Some(Ok(msg)) = receiver.next().await {
            match msg {
                Message::Text(text) => {
                    // Parse client message
                    if let Ok(client_msg) = serde_json::from_str::<ClientMessage>(&text) {
                        match client_msg {
                            ClientMessage::MovieLiked {
                                participant_id,
                                imdb_id,
                            } => {
                                info!("Participant {} liked movie {}", participant_id, imdb_id);

                                if let Some(mut room) =
                                    state_for_recv.rooms.get_mut(&room_id_for_recv)
                                {
                                    room.add_like(&participant_id, imdb_id.clone());

                                    // Broadcast updated likes to all participants
                                    let all_likes = room.get_all_participant_likes(&all_movies);
                                    let common_likes = room.get_common_likes(&all_movies);

                                    if let Some(tx) = &room.tx {
                                        let _ =
                                            tx.send(crate::models::ServerMessage::LikesUpdated {
                                                all_likes: all_likes.clone(),
                                                common_likes: common_likes.clone(),
                                            });
                                    }

                                    // Check for match (3+ common movies)
                                    if common_likes.len() >= 3 {
                                        info!("Match found! {} common movies", common_likes.len());

                                        // Broadcast match found
                                        if let Some(tx) = &room.tx {
                                            let _ =
                                                tx.send(crate::models::ServerMessage::MatchFound {
                                                    all_likes,
                                                    common_likes,
                                                });
                                        }
                                        // Room stays active - dont end matching
                                    }
                                }
                            }
                            ClientMessage::EndMatching => {
                                info!(
                                    "Manual end matching requested for room {}",
                                    room_id_for_recv
                                );

                                if let Some(mut room) =
                                    state_for_recv.rooms.get_mut(&room_id_for_recv)
                                {
                                    room.end_matching(&all_movies);
                                }
                                break;
                            }
                        }
                    }
                }
                Message::Binary(data) => {
                    // Accept movie data from client for caching
                    if let Ok(movies) = serde_json::from_slice::<Vec<MovieData>>(&data) {
                        all_movies.extend(movies);
                        // Remove duplicates
                        all_movies.sort_by(|a, b| a.imdb_id.cmp(&b.imdb_id));
                        all_movies.dedup_by(|a, b| a.imdb_id == b.imdb_id);
                    }
                }
                Message::Ping(_) => {}
                Message::Close(_) => {
                    break;
                }
                _ => {}
            }
        }
    });

    // Wait for either task to finish
    tokio::select! {
        _ = (&mut send_task) => recv_task.abort(),
        _ = (&mut recv_task) => send_task.abort(),
    }

    info!("WebSocket connection closed for room {}", room_id);
}
