use dashmap::DashMap;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use tokio::sync::broadcast;
use uuid::Uuid;

#[derive(Clone)]
pub struct AppState {
    pub rooms: DashMap<String, Room>,
    pub omdb_api_key: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Room {
    pub id: String,
    pub filters: RoomFilters,
    pub participants: Vec<String>,
    pub is_active: bool,
    pub host_id: String,
    #[serde(skip)]
    pub tx: Option<broadcast::Sender<ServerMessage>>,
    #[serde(skip)]
    pub participant_likes: HashMap<String, Vec<String>>, // participant_id -> vec of imdb_ids
    #[serde(skip)]
    pub sent_movie_ids: std::collections::HashSet<String>, // track sent movies to avoid duplicates
    #[serde(skip)]
    pub current_page: u32, // track current OMDB page for pagination
}

impl Room {
    pub fn new(filters: RoomFilters, host_id: String) -> Self {
        let (tx, _) = broadcast::channel(100);
        let mut participant_likes = HashMap::new();
        participant_likes.insert(host_id.clone(), Vec::new());

        Self {
            id: Uuid::new_v4().to_string(),
            filters,
            participants: vec![host_id.clone()],
            is_active: false,
            host_id: host_id.clone(),
            tx: Some(tx),
            participant_likes,
            sent_movie_ids: std::collections::HashSet::new(),
            current_page: 1,
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RoomFilters {
    pub genre: Option<String>,
    pub year_from: Option<u32>,
    pub year_to: Option<u32>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ParticipantLikes {
    pub participant_id: String,
    pub liked_movies: Vec<MovieData>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(tag = "type")]
pub enum ServerMessage {
    ParticipantJoined {
        participant_id: String,
    },
    ParticipantLeft {
        participant_id: String,
    },
    MatchingStarted,
    NewMovie {
        movie: MovieData,
    },
    LikesUpdated {
        all_likes: Vec<ParticipantLikes>,
        common_likes: Vec<MovieData>,
    },
    MatchingEnded {
        all_likes: Vec<ParticipantLikes>,
        common_likes: Vec<MovieData>,
    },
    StreamingEnded,
    MatchFound {
        all_likes: Vec<ParticipantLikes>,
        common_likes: Vec<MovieData>,
    },
    Error {
        message: String,
    },
}

#[derive(Debug, Clone, Deserialize)]
#[serde(tag = "type")]
pub enum ClientMessage {
    MovieLiked {
        participant_id: String,
        imdb_id: String,
    },
    EndMatching,
    LeaveRoom { participant_id: String },
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MovieData {
    pub title: String,
    pub year: String,
    pub poster: String,
    pub plot: String,
    pub genre: String,
    pub imdb_rating: String,
    pub imdb_id: String,
}

#[derive(Debug, Deserialize)]
pub struct CreateRoomRequest {
    pub host_id: String,
    pub filters: RoomFilters,
}

#[derive(Debug, Serialize)]
pub struct CreateRoomResponse {
    pub room_id: String,
    pub join_url: String,
}

#[derive(Debug, Deserialize)]
pub struct JoinRoomRequest {
    pub participant_id: String,
}

#[derive(Debug, Serialize)]
pub struct JoinRoomResponse {
    pub success: bool,
    pub room: Option<RoomInfo>,
}

#[derive(Debug, Serialize)]
pub struct RoomInfo {
    pub id: String,
    pub filters: RoomFilters,
    pub participants_count: usize,
    pub is_active: bool,
}

#[derive(Debug, Serialize)]
pub struct RoomState {
    pub id: String,
    pub filters: RoomFilters,
    pub participants: Vec<String>,
    pub is_active: bool,
    pub host_id: String,
    pub all_likes: Vec<ParticipantLikes>,
    pub common_likes: Vec<MovieData>,
}
