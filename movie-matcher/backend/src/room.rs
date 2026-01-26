use crate::models::{MovieData, ParticipantLikes, Room, ServerMessage};
use std::collections::{HashMap, HashSet};
use tracing::info;

impl Room {
    pub fn add_participant(&mut self, participant_id: String) -> bool {
        if !self.participants.contains(&participant_id) {
            self.participants.push(participant_id.clone());
            self.participant_likes
                .insert(participant_id.clone(), Vec::new());

            // Broadcast to all participants
            if let Some(tx) = &self.tx {
                let _ = tx.send(ServerMessage::ParticipantJoined { participant_id });
            }

            true
        } else {
            false
        }
    }

    pub fn _remove_participant(&mut self, participant_id: &str) {
        if let Some(pos) = self.participants.iter().position(|p| p == participant_id) {
            self.participants.remove(pos);
            self.participant_likes.remove(participant_id);

            // Broadcast to all participants
            if let Some(tx) = &self.tx {
                let _ = tx.send(ServerMessage::ParticipantLeft {
                    participant_id: participant_id.to_string(),
                });
            }
        }
    }

    pub fn start(&mut self) {
        self.is_active = true;
        // Reset likes and sent movies when starting
        for likes in self.participant_likes.values_mut() {
            likes.clear();
        }
        self.sent_movie_ids.clear();

        if let Some(tx) = &self.tx {
            let _ = tx.send(ServerMessage::MatchingStarted);
        }
        info!("Room {} started matching", self.id);
    }

    pub fn broadcast_movie(&self, movie: crate::models::MovieData) {
        if let Some(tx) = &self.tx {
            let _ = tx.send(ServerMessage::NewMovie { movie });
        }
    }

    pub fn add_like(&mut self, participant_id: &str, imdb_id: String) {
        if let Some(likes) = self.participant_likes.get_mut(participant_id) {
            if !likes.contains(&imdb_id) {
                likes.push(imdb_id);
            }
        }
    }

    pub fn check_for_match(&self, all_movies: &[MovieData]) -> Option<Vec<MovieData>> {
        // Need at least 2 participants
        if self.participants.len() < 2 {
            return None;
        }

        // Find movies liked by all participants
        let mut movie_counts: HashMap<String, usize> = HashMap::new();

        for likes in self.participant_likes.values() {
            let unique_likes: HashSet<_> = likes.iter().collect();
            for imdb_id in unique_likes {
                *movie_counts.entry(imdb_id.clone()).or_insert(0) += 1;
            }
        }

        // Find movies liked by ALL participants
        let num_participants = self.participants.len();
        let common_movie_ids: Vec<String> = movie_counts
            .into_iter()
            .filter(|(_, count)| *count == num_participants)
            .map(|(id, _)| id)
            .collect();

        // If we have 3 or more common movies, we have a match!
        if common_movie_ids.len() >= 3 {
            let matched_movies: Vec<MovieData> = all_movies
                .iter()
                .filter(|m| common_movie_ids.contains(&m.imdb_id))
                .cloned()
                .collect();

            if matched_movies.len() >= 3 {
                info!(
                    "Room {}: Match found with {} common movies!",
                    self.id,
                    matched_movies.len()
                );
                return Some(matched_movies);
            }
        }

        None
    }

    pub fn get_common_likes(&self, all_movies: &[MovieData]) -> Vec<MovieData> {
        if self.participants.len() < 2 {
            return Vec::new();
        }

        let mut movie_counts: HashMap<String, usize> = HashMap::new();

        for likes in self.participant_likes.values() {
            let unique_likes: HashSet<_> = likes.iter().collect();
            for imdb_id in unique_likes {
                *movie_counts.entry(imdb_id.clone()).or_insert(0) += 1;
            }
        }

        let num_participants = self.participants.len();
        let common_movie_ids: Vec<String> = movie_counts
            .into_iter()
            .filter(|(_, count)| *count == num_participants)
            .map(|(id, _)| id)
            .collect();

        all_movies
            .iter()
            .filter(|m| common_movie_ids.contains(&m.imdb_id))
            .cloned()
            .collect()
    }

    pub fn get_all_participant_likes(&self, all_movies: &[MovieData]) -> Vec<ParticipantLikes> {
        self.participants
            .iter()
            .map(|participant_id| {
                let liked_movie_ids = self
                    .participant_likes
                    .get(participant_id)
                    .cloned()
                    .unwrap_or_default();

                let liked_movies: Vec<MovieData> = all_movies
                    .iter()
                    .filter(|m| liked_movie_ids.contains(&m.imdb_id))
                    .cloned()
                    .collect();

                ParticipantLikes {
                    participant_id: participant_id.clone(),
                    liked_movies,
                }
            })
            .collect()
    }

    pub fn end_matching(&mut self, all_movies: &[MovieData]) {
        self.is_active = false;
        if let Some(tx) = &self.tx {
            let all_likes = self.get_all_participant_likes(all_movies);
            let common_likes = self.get_common_likes(all_movies);

            let _ = tx.send(ServerMessage::MatchingEnded {
                all_likes,
                common_likes,
            });
        }
        info!("Room {} ended matching", self.id);
    }
}
