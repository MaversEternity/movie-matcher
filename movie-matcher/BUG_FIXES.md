# Bug Fixes

## Bug #1: Host Likes Not Appearing ✅

### Problem
When the host liked movies, their likes weren't showing in the sidebar. Other participants' likes displayed fine.

### Root Cause
When creating a room, the host was added to the `participants` array but not to the `participant_likes` HashMap.

### Fix
**File**: `backend/src/models.rs`

Updated `Room::new()` to initialize `participant_likes` with the host:

```rust
pub fn new(filters: RoomFilters, host_id: String) -> Self {
    let (tx, _) = broadcast::channel(100);
    let mut participant_likes = HashMap::new();
    participant_likes.insert(host_id.clone(), Vec::new()); // ✅ Initialize host
    
    Self {
        // ... other fields
        participant_likes,
    }
}
```

### Result
✅ Host's likes now appear in sidebar immediately
✅ All participants' likes are tracked equally

---

## Bug #2: State Lost on Page Reload ✅

### Problem
When a user reloaded the page, they couldn't rejoin the room and lost all progress.

### Solution
Implemented session persistence using localStorage.

### Changes

**File**: `frontend/src/App.vue`

1. **Save Session on Join/Create**:
```javascript
function handleRoomCreated(data) {
  // ... existing code
  localStorage.setItem(`room_${roomId.value}`, JSON.stringify({
    participantId: data.hostId,
    isHost: true
  }))
}
```

2. **Restore Session on Mount**:
```javascript
onMounted(async () => {
  const match = path.match(/\/room\/([a-f0-9-]+)/)
  if (match) {
    const savedSession = localStorage.getItem(`room_${roomId.value}`)
    if (savedSession) {
      // Restore participant ID and role
      // Verify room still exists
      // Auto-rejoin if valid
    }
  }
})
```

**File**: `backend/src/handlers.rs`

Added `/api/rooms/:room_id/state` endpoint to get current room state.

### Result
✅ Users can reload and automatically rejoin
✅ Session persists until browser data is cleared
✅ Validates room still exists before rejoining

---

## Bug #3: Duplicate Movies & No End ✅

### Problem
1. Same movies appeared multiple times
2. When OMDB API had no more unique movies, streaming continued indefinitely

### Solution
Track sent movies and gracefully end when exhausted.

### Changes

**File**: `backend/src/models.rs`

Added tracking field to Room:
```rust
pub struct Room {
    // ... other fields
    pub sent_movie_ids: std::collections::HashSet<String>, // Track sent movies
}
```

**File**: `backend/src/room.rs`

Clear tracking on start:
```rust
pub fn start(&mut self) {
    // Reset likes and sent movies when starting
    self.sent_movie_ids.clear();
}
```

**File**: `backend/src/handlers.rs`

Implemented smart streaming with deduplication:
```rust
async fn stream_movies_infinitely() {
    let mut failed_attempts = 0;
    let max_failed_attempts = 3;
    
    loop {
        match fetch_movies() {
            Ok(movies) => {
                // Filter out duplicates
                let new_movies = movies.filter(|m| {
                    !room.sent_movie_ids.contains(&m.imdb_id)
                });
                
                if new_movies.is_empty() {
                    failed_attempts++;
                    if failed_attempts >= 3 {
                        // End matching gracefully
                        room.end_matching();
                        break;
                    }
                } else {
                    // Add to sent set
                    for movie in &new_movies {
                        room.sent_movie_ids.insert(movie.imdb_id);
                    }
                    // Broadcast movies
                }
            }
        }
    }
}
```

### Result
✅ No duplicate movies shown
✅ Automatically ends when no more unique movies available
✅ Shows results page after 3 failed attempts to find new movies
✅ Graceful degradation instead of infinite loop

---

## Testing

### Backend
```bash
cd backend
cargo build
# ✅ Compiles successfully (2.56s)
```

### Frontend
```bash
cd frontend
npm run build
# ✅ Builds successfully (318ms)
```

### To Test

1. **Stop current servers** (Ctrl+C both terminals)

2. **Restart**:
```bash
# Terminal 1
./start-backend.sh

# Terminal 2
./start-frontend.sh
```

3. **Test Bug #1 (Host Likes)**:
   - Create room as Alice
   - Start matching
   - Like some movies
   - ✅ Should see Alice's likes in sidebar immediately

4. **Test Bug #2 (Reload)**:
   - Join a room
   - Reload the page (F5)
   - ✅ Should automatically rejoin the room
   - ✅ Should maintain your participant ID

5. **Test Bug #3 (Duplicates)**:
   - Start matching
   - Swipe through many movies
   - ✅ Should never see the same movie twice
   - Keep swiping until API exhausted
   - ✅ Should eventually show results page automatically

---

## Summary

All three bugs fixed:

1. ✅ **Host Likes**: Now tracked properly from room creation
2. ✅ **Page Reload**: Session persistence with localStorage
3. ✅ **Duplicates**: HashSet tracking prevents duplicates, auto-ends when exhausted

The app now handles edge cases gracefully and provides a smooth user experience!
