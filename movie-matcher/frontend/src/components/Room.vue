<template>
  <div class="room">
    <div class="card">
      <div class="room-header">
        <h2>Movie Matching Room</h2>
        <div class="room-status">
          <span class="status-badge" :class="{ active: isMatchingActive }">
            {{ isMatchingActive ? 'Matching Active' : 'Waiting' }}
          </span>
          <span class="participants">👥 {{ participants.length }} participant(s)</span>
        </div>
      </div>

      <div class="room-content">
        <!-- Pre-matching: Host controls -->
        <div v-if="!isMatchingActive && !matchingEnded && isHost" class="start-section">
          <p class="info-text">Share this URL with your friends:</p>
          <div class="share-link">
            <input
              ref="linkInput"
              :value="shareUrl"
              readonly
              @click="selectAll"
            />
            <button @click="copyLink" class="btn btn-secondary">
              {{ copied ? 'Copied!' : 'Copy' }}
            </button>
          </div>

          <button @click="startMatching" class="btn btn-primary btn-large" :disabled="starting">
            {{ starting ? 'Starting...' : 'Start Matching' }}
          </button>
        </div>

        <!-- Pre-matching: Participant waiting -->
        <div v-if="!isMatchingActive && !matchingEnded && !isHost" class="waiting-section">
          <p class="info-text">Waiting for host to start matching...</p>
          <div class="loader"></div>
        </div>

        <!-- Active matching -->
        <div v-if="isMatchingActive && !matchingEnded" class="matching-section">
          <!-- Current movie -->
          <div v-if="currentMovie" class="movie-container">
            <div class="movie-counter">{{ currentMovieIndex + 1 }} / {{ totalMovies }}</div>

            <div class="movie-card">
              <div class="movie-poster">
                <img
                  v-if="currentMovie.poster"
                  :src="currentMovie.poster"
                  :alt="currentMovie.title"
                />
                <div v-else class="no-poster">No Poster</div>
              </div>

              <div class="movie-details">
                <h3>{{ currentMovie.title }}</h3>
                <div class="movie-meta">
                  <span v-if="currentMovie.year" class="badge">📅 {{ currentMovie.year }}</span>
                  <span v-if="currentMovie.rated" class="badge">{{ currentMovie.rated }}</span>
                  <span v-if="currentMovie.runtime" class="badge">⏱ {{ currentMovie.runtime }}</span>
                  <span v-if="currentMovie.imdb_rating" class="badge">⭐ {{ currentMovie.imdb_rating }}</span>
                </div>
                <div class="movie-info">
                  <p v-if="currentMovie.genre"><strong>Genre:</strong> {{ currentMovie.genre }}</p>
                  <p v-if="currentMovie.director"><strong>Director:</strong> {{ currentMovie.director }}</p>
                  <p v-if="currentMovie.actors"><strong>Actors:</strong> {{ currentMovie.actors }}</p>
                  <p v-if="currentMovie.country"><strong>Country:</strong> {{ currentMovie.country }}</p>
                </div>
                <p v-if="currentMovie.plot" class="movie-plot">{{ currentMovie.plot }}</p>
              </div>
            </div>
            <div class="swipe-controls">
              <button @click="swipeLeft" class="swipe-btn swipe-left" title="Not interested">
                <span class="swipe-icon">👎</span>
                <span class="swipe-text">Pass</span>
              </button>
              <button @click="swipeRight" class="swipe-btn swipe-right" title="Interested">
                <span class="swipe-icon">👍</span>
                <span class="swipe-text">Like</span>
              </button>
            </div>

            <p class="swipe-hint">Keep swiping until you find 3 common favorites!</p>
          </div>

          <div v-else class="loading-movie">
            <div class="loader"></div>
            <p>Loading next movie...</p>
          </div>

          <!-- Live Likes Sidebar -->
          <div class="likes-sidebar">
            <div class="sidebar-header">
              <h4>Live Likes</h4>
              <span class="common-count" v-if="commonLikes.length > 0">
                {{ commonLikes.length }} common
              </span>
            </div>

            <!-- Common Likes -->
            <div v-if="commonLikes.length > 0" class="common-likes-section">
              <h5>✨ Common Favorites ({{ commonLikes.length }})</h5>
              <div class="common-movies-grid">
                <div v-for="movie in commonLikes" :key="movie.imdb_id" class="common-movie-mini">
                  <img v-if="movie.poster !== 'N/A'" :src="movie.poster" :alt="movie.title" />
                  <div class="common-movie-info">
                    <strong>{{ movie.title }}</strong>
                    <span>{{ movie.year }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- Each Participant's Likes -->
            <div class="all-likes-section">
              <div v-for="participantLikes in allParticipantLikes" :key="participantLikes.participant_id" class="participant-likes-group">
                <h5>
                  {{ participantLikes.participant_id }}
                  {{ participantLikes.participant_id === participantId ? '(you)' : '' }}
                  <span class="like-count">{{ participantLikes.liked_movies.length }} likes</span>
                </h5>
                <div v-if="participantLikes.liked_movies.length > 0" class="mini-movies-list">
                  <div v-for="movie in participantLikes.liked_movies" :key="movie.imdb_id" class="mini-movie">
                    <img v-if="movie.poster !== 'N/A'" :src="movie.poster" :alt="movie.title" />
                    <span>{{ movie.title }}</span>
                  </div>
                </div>
                <p v-else class="no-likes">No likes yet</p>
              </div>
            </div>
          </div>

          <!-- Exit Room Button (Non-host participants) -->
          <div v-if="!isHost" class="exit-room-section">
            <button @click="exitRoom" class="btn btn-warning">
              🚪 Exit Room
            </button>
          </div>

          <!-- End Matching Button (Host only) -->
          <div v-if="isHost" class="end-matching-section">
            <button @click="endMatching" class="btn btn-danger">
              ⏹ End Matching
            </button>
          </div>
        </div>

        <!-- Results page -->
        <div v-if="matchingEnded" class="ended-section">
          <div v-if="commonLikes.length >= 3" class="match-celebration">
            <h3>🎉 Perfect Match!</h3>
            <p class="celebration-text">You all loved these {{ commonLikes.length }} movies!</p>
          </div>
          <div v-else-if="commonLikes.length > 0" class="partial-match">
            <h3>🎬 Close Match!</h3>
            <p class="celebration-text">You found {{ commonLikes.length }} movie(s) in common. Maybe try a few more?</p>
          </div>
          <div v-else class="no-match">
            <h3>📽️ Different Tastes!</h3>
            <p class="celebration-text">No common favorites yet. See what everyone liked below!</p>
          </div>

          <!-- Common Likes Section -->
          <div v-if="commonLikes.length > 0" class="results-section common-section">
            <h4>✨ Movies You All Loved</h4>
            <div class="results-grid">
              <div v-for="movie in commonLikes" :key="movie.imdb_id" class="result-movie-card common-movie">
                <img v-if="movie.poster !== 'N/A'" :src="movie.poster" :alt="movie.title" class="result-poster" />
                <div v-else class="result-no-poster">No Poster</div>
                <div class="result-info">
                  <strong>{{ movie.title }}</strong>
                  <div class="result-meta">
                    <span class="badge">{{ movie.year }}</span>
                    <span class="badge">{{ movie.genre }}</span>
                    <span class="badge">⭐ {{ movie.imdb_rating }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Each Participant's Likes -->
          <div class="results-section individual-section">
            <h4>📋 Everyone's Favorites</h4>
            <div v-for="participantLikes in allParticipantLikes" :key="participantLikes.participant_id" class="participant-results">
              <h5 class="participant-name">
                {{ participantLikes.participant_id }}
                {{ participantLikes.participant_id === participantId ? '(you)' : '' }}
                <span class="badge-count">{{ participantLikes.liked_movies.length }} movies</span>
              </h5>

              <div v-if="participantLikes.liked_movies.length > 0" class="results-grid">
                <div v-for="movie in participantLikes.liked_movies" :key="movie.imdb_id" class="result-movie-card">
                  <img v-if="movie.poster !== 'N/A'" :src="movie.poster" :alt="movie.title" class="result-poster" />
                  <div v-else class="result-no-poster">No Poster</div>
                  <div class="result-info">
                    <strong>{{ movie.title }}</strong>
                    <div class="result-meta">
                      <span class="badge">{{ movie.year }}</span>
                      <span class="badge">⭐ {{ movie.imdb_rating }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <p v-else class="no-results">No movies liked</p>
            </div>
          </div>

          <!-- Restart Matching Section (Host only) -->
          <div v-if="isHost" class="restart-section">
            <h4>🔄 Try Again?</h4>
            <p>Adjust your filters and start a new matching session</p>

            <div class="restart-form">
              <div class="form-group">
                <label for="restart-genre">Genre (optional)</label>
                <select id="restart-genre" v-model="restartFilters.genre">
                  <option value="">Any Genre</option>
                  <option value="Action">Action</option>
                  <option value="Comedy">Comedy</option>
                  <option value="Drama">Drama</option>
                  <option value="Horror">Horror</option>
                  <option value="Romance">Romance</option>
                  <option value="Sci-Fi">Sci-Fi</option>
                  <option value="Thriller">Thriller</option>
                  <option value="Adventure">Adventure</option>
                  <option value="Animation">Animation</option>
                </select>
              </div>

              <div class="form-row">
                <div class="form-group">
                  <label for="restart-year-from">Year From</label>
                  <input
                    id="restart-year-from"
                    v-model.number="restartFilters.year_from"
                    type="number"
                    min="1900"
                    max="2030"
                    placeholder="e.g., 2000"
                  />
                </div>

                <div class="form-group">
                  <label for="restart-year-to">Year To</label>
                  <input
                    id="restart-year-to"
                    v-model.number="restartFilters.year_to"
                    type="number"
                    min="1900"
                    max="2030"
                    placeholder="e.g., 2024"
                  />
                </div>
              </div>

              <button @click="restartMatching" class="btn btn-primary btn-large" :disabled="restarting">
                {{ restarting ? 'Restarting...' : '🎬 Start New Matching' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="error" class="error">{{ error }}</div>

      <div class="participants-list">
        <h4>Participants:</h4>
        <ul>
          <li v-for="p in participants" :key="p">
            {{ p }} {{ p === participantId ? '(you)' : '' }}
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  roomId: String,
  participantId: String,
  isHost: Boolean
})

const isMatchingActive = ref(false)
const matchingEnded = ref(false)
const streamingEnded = ref(false)
const currentMovie = ref(null)
const movieQueue = ref([])
const currentMovieIndex = ref(0)
const totalMovies = ref(0)
const allParticipantLikes = ref([])
const commonLikes = ref([])
const participants = ref([props.participantId])
const starting = ref(false)
const restarting = ref(false)
const error = ref('')
const shareUrl = ref('')
const copied = ref(false)
const linkInput = ref(null)
const restartFilters = ref({
  genre: '',
  year_from: null,
  year_to: null
})

let ws = null

onMounted(async () => {
  shareUrl.value = `${window.location.origin}/room/${props.roomId}`
  await fetchRoomState()
  connectWebSocket()
})

async function fetchRoomState() {
  try {
    const response = await fetch(`/api/rooms/${props.roomId}/state`)
    if (response.ok) {
      const roomState = await response.json()
      participants.value = roomState.participants || []
    }
  } catch (err) {
    console.error("Error fetching room state:", err)
  }
}

onUnmounted(() => {
  if (ws) {
    ws.close()
  }
})

function connectWebSocket() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//${window.location.hostname}:3000/api/rooms/${props.roomId}/ws`

  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    console.log('WebSocket connected')
  }

  ws.onmessage = (event) => {
    try {
      const message = JSON.parse(event.data)
      handleMessage(message)
    } catch (err) {
      console.error('Error parsing message:', err)
    }
  }

  ws.onerror = (err) => {
    console.error('WebSocket error:', err)
    error.value = 'Connection error'
  }

  ws.onclose = () => {
    console.log('WebSocket disconnected')
  }
}

function handleMessage(message) {
  switch (message.type) {
    case 'ParticipantJoined':
      if (!participants.value.includes(message.participant_id)) {
        participants.value.push(message.participant_id)
      }
      break

    case 'ParticipantLeft':
      participants.value = participants.value.filter(p => p !== message.participant_id)
      break

    case 'MatchingStarted':
      isMatchingActive.value = true
      matchingEnded.value = false
      currentMovie.value = null
      movieQueue.value = []
      currentMovieIndex.value = 0
      allParticipantLikes.value = []
      streamingEnded.value = false
      commonLikes.value = []
      break

    case 'NewMovie':
      // Add movie to queue
      movieQueue.value.push(message.movie)
      totalMovies.value = movieQueue.value.length

      // Send movie to backend for caching
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(new Blob([JSON.stringify([message.movie])]))
      }

      // Show first movie if none is currently shown
      if (!currentMovie.value) {
        showNextMovie()
      }
      break

    case 'LikesUpdated':
      // Update live likes display
      allParticipantLikes.value = message.all_likes
      commonLikes.value = message.common_likes
      break

    case 'StreamingEnded':
      // Backend has exhausted all pages, no more movies coming
      streamingEnded.value = true
      // Check if queue is empty and end matching
      if (currentMovieIndex.value >= movieQueue.value.length) {
        // Queue is empty, fetch final state and end matching
        endMatchingDueToExhaustion()
      }
      break

    case 'MatchFound':
      // Match found! Show results
      allParticipantLikes.value = message.all_likes
      commonLikes.value = message.common_likes
      isMatchingActive.value = false
      matchingEnded.value = true
      currentMovie.value = null
      console.log('Match found!', message.common_likes)
      break

    case 'MatchingEnded':
      // Matching ended manually or no more movies
      allParticipantLikes.value = message.all_likes
      commonLikes.value = message.common_likes
      isMatchingActive.value = false
      matchingEnded.value = true
      currentMovie.value = null
      break

    case 'Error':
      error.value = message.message
      break
  }
}

function showNextMovie() {
  if (currentMovieIndex.value < movieQueue.value.length) {
    currentMovie.value = movieQueue.value[currentMovieIndex.value]
  } else {
    // Wait for more movies (infinite streaming)
    // Check if streaming has ended
    if (streamingEnded.value) {
      // No more movies coming, end matching
      endMatchingDueToExhaustion()
    } else {
      // Wait for more movies (infinite streaming)
      currentMovie.value = null
    }
  }
}

function swipeLeft() {
  // Pass on this movie
  currentMovieIndex.value++
  showNextMovie()
}

function swipeRight() {
  // Like this movie
  if (currentMovie.value) {
    // Send like to backend
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({
        type: 'MovieLiked',
        participant_id: props.participantId,
        imdb_id: currentMovie.value.imdb_id
      }))
    }
  }

  currentMovieIndex.value++
  showNextMovie()
}

function exitRoom() {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({
      type: 'LeaveRoom',
      participant_id: props.participantId
    }))
  }
  // Clear local storage and redirect to home
  localStorage.removeItem(`room_${props.roomId}`)
  window.location.href = '/'
}

function endMatching() {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({
      type: 'EndMatching'
    }))
  }
}

async function endMatchingDueToExhaustion() {
  // Fetch final state from backend
  try {
    const response = await fetch(`/api/rooms/${props.roomId}/state`)
    if (response.ok) {
      const roomState = await response.json()
      allParticipantLikes.value = roomState.all_likes || []
      commonLikes.value = roomState.common_likes || []
    }
  } catch (err) {
    console.error("Error fetching final state:", err)
  }
  
  // Show results page
  isMatchingActive.value = false
  matchingEnded.value = true
  currentMovie.value = null
}

async function startMatching() {
  starting.value = true
  error.value = ''

  try {
    const response = await fetch(`/api/rooms/${props.roomId}/start`, {
      method: 'POST'
    })

    if (!response.ok) {
      throw new Error('Failed to start matching')
    }
  } catch (err) {
    error.value = err.message
  } finally {
    starting.value = false
  }
}

async function restartMatching() {
  restarting.value = true
  error.value = ''

  try {
    // Update room filters
    const updateResponse = await fetch(`/api/rooms/${props.roomId}/filters`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        genre: restartFilters.value.genre || null,
        year_from: restartFilters.value.year_from || null,
        year_to: restartFilters.value.year_to || null,
      })
    })

    if (!updateResponse.ok) {
      throw new Error('Failed to update filters')
    }

    // Reset state
    matchingEnded.value = false
    movieQueue.value = []
    currentMovieIndex.value = 0
    totalMovies.value = 0
    allParticipantLikes.value = []
      streamingEnded.value = false
    commonLikes.value = []
    currentMovie.value = null

    // Start new matching session
    const startResponse = await fetch(`/api/rooms/${props.roomId}/start`, {
      method: 'POST'
    })

    if (!startResponse.ok) {
      throw new Error('Failed to start matching')
    }
  } catch (err) {
    error.value = err.message
  } finally {
    restarting.value = false
  }
}

function selectAll(event) {
  event.target.select()
}

async function copyLink() {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    copied.value = true
    setTimeout(() => {
      copied.value = false
    }, 2000)
  } catch (err) {
    linkInput.value.select()
    document.execCommand('copy')
    copied.value = true
    setTimeout(() => {
      copied.value = false
    }, 2000)
  }
}
</script>

<style scoped>
.room {
  width: 100%;
  max-width: 1400px;
}

.card {
  background: white;
  border-radius: 1rem;
  padding: 2rem;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.room-header {
  margin-bottom: 2rem;
  text-align: center;
}

h2 {
  color: #667eea;
  margin-bottom: 1rem;
}

.room-status {
  display: flex;
  justify-content: center;
  gap: 1rem;
  align-items: center;
}

.status-badge {
  padding: 0.5rem 1rem;
  background: #e0e0e0;
  border-radius: 2rem;
  font-size: 0.9rem;
  font-weight: 600;
}

.status-badge.active {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.participants {
  color: #666;
}

.room-content {
  min-height: 400px;
}

.start-section, .waiting-section {
  text-align: center;
  padding: 2rem 0;
}

.info-text {
  margin-bottom: 1rem;
  color: #666;
}

.share-link {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 2rem;
}

.share-link input {
  flex: 1;
  padding: 0.75rem;
  border: 2px solid #e0e0e0;
  border-radius: 0.5rem;
  font-size: 0.9rem;
}

.btn {
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 0.5rem;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 5px 20px rgba(102, 126, 234, 0.4);
}

.btn-secondary {
  background: #6c757d;
  color: white;
}

.btn-secondary:hover {
  background: #5a6268;
}

.btn-danger {
  background: #dc3545;
  color: white;
}

.btn-danger:hover {
  background: #c82333;
}

.btn-large {
  padding: 1rem 2rem;
  font-size: 1.1rem;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.loader {
  border: 4px solid #f3f3f3;
  border-top: 4px solid #667eea;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin: 2rem auto;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* Matching Section */
.matching-section {
  display: grid;
  grid-template-columns: 1fr 350px;
  gap: 2rem;
  align-items: start;
}

.movie-container {
  position: relative;
}

.movie-counter {
  text-align: center;
  font-size: 0.9rem;
  color: #888;
  margin-bottom: 1rem;
  font-weight: 600;
}

.movie-card {
  display: flex;
  gap: 2rem;
  padding: 1.5rem;
  background: #f8f9fa;
  border-radius: 0.5rem;
  margin-bottom: 1.5rem;
}

.movie-poster {
  flex-shrink: 0;
  width: 200px;
}

.movie-poster img {
  width: 100%;
  border-radius: 0.5rem;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.no-poster {
  width: 200px;
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e0e0e0;
  border-radius: 0.5rem;
  color: #999;
}

.movie-details {
  flex: 1;
}

.movie-details h3 {
  margin-bottom: 1rem;
  color: #333;
  font-size: 1.5rem;
}

.movie-meta {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.badge {
  padding: 0.25rem 0.75rem;
  background: white;
  border-radius: 1rem;
  font-size: 0.85rem;
  color: #666;
}

.movie-plot {
  color: #555;
  line-height: 1.6;
}

.movie-info {
  margin: 1rem 0;
  padding: 0.75rem;
  background: #f8f9fa;
  border-radius: 0.5rem;
}

.movie-info p {
  margin: 0.5rem 0;
  font-size: 0.9rem;
  color: #555;
}

.movie-info strong {
  color: #333;
  margin-right: 0.5rem;
}

.swipe-controls {
  display: flex;
  justify-content: center;
  gap: 2rem;
  margin-bottom: 0.5rem;
}

.swipe-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1.5rem 2.5rem;
  border: none;
  border-radius: 1rem;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  min-width: 120px;
}

.swipe-btn .swipe-icon {
  font-size: 2.5rem;
}

.swipe-left {
  background: #ff4757;
  color: white;
}

.swipe-left:hover {
  background: #ee5a6f;
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 5px 20px rgba(255, 71, 87, 0.4);
}

.swipe-right {
  background: #2ed573;
  color: white;
}

.swipe-right:hover {
  background: #26de81;
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 5px 20px rgba(46, 213, 115, 0.4);
}

.swipe-hint {
  text-align: center;
  font-size: 0.85rem;
  color: #888;
  font-style: italic;
}

.loading-movie {
  text-align: center;
  padding: 3rem 0;
}

/* Likes Sidebar */
.likes-sidebar {
  background: linear-gradient(135deg, #f8f9fa 0%, #e9ecef 100%);
  border-radius: 1rem;
  padding: 1.5rem;
  max-height: 80vh;
  overflow-y: auto;
  position: sticky;
  top: 2rem;
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #dee2e6;
}

.sidebar-header h4 {
  margin: 0;
  color: #495057;
}

.common-count {
  background: #2ed573;
  color: white;
  padding: 0.25rem 0.75rem;
  border-radius: 1rem;
  font-size: 0.85rem;
  font-weight: 600;
}

.common-likes-section {
  margin-bottom: 1.5rem;
  padding: 1rem;
  background: white;
  border-radius: 0.75rem;
  border: 2px solid #2ed573;
}

.common-likes-section h5 {
  margin: 0 0 1rem 0;
  color: #2ed573;
  font-size: 0.9rem;
}

.common-movies-grid {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.common-movie-mini {
  display: flex;
  gap: 0.75rem;
  align-items: center;
  background: #f8f9fa;
  padding: 0.5rem;
  border-radius: 0.5rem;
}

.common-movie-mini img {
  width: 40px;
  height: 60px;
  object-fit: cover;
  border-radius: 0.25rem;
}

.common-movie-info {
  flex: 1;
  font-size: 0.85rem;
}

.common-movie-info strong {
  display: block;
  color: #333;
  margin-bottom: 0.25rem;
}

.common-movie-info span {
  color: #666;
  font-size: 0.75rem;
}

.all-likes-section {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.participant-likes-group {
  background: white;
  padding: 1rem;
  border-radius: 0.75rem;
}

.participant-likes-group h5 {
  margin: 0 0 0.75rem 0;
  color: #495057;
  font-size: 0.9rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.like-count {
  background: #667eea;
  color: white;
  padding: 0.2rem 0.6rem;
  border-radius: 1rem;
  font-size: 0.75rem;
}

.mini-movies-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.mini-movie {
  display: flex;
  gap: 0.5rem;
  align-items: center;
  padding: 0.4rem;
  background: #f8f9fa;
  border-radius: 0.4rem;
  font-size: 0.8rem;
}

.mini-movie img {
  width: 30px;
  height: 45px;
  object-fit: cover;
  border-radius: 0.25rem;
}

.mini-movie span {
  flex: 1;
  color: #495057;
}

.no-likes {
  text-align: center;
  color: #adb5bd;
  font-size: 0.85rem;
  margin: 0;
  padding: 0.5rem;
}

.end-matching-section {
  grid-column: 1 / -1;
  text-align: center;
  margin-top: 1rem;
}

/* Results Section */
.ended-section {
  padding: 2rem 0;
}

.match-celebration,
.partial-match,
.no-match {
  text-align: center;
  padding: 2rem;
  border-radius: 1rem;
  margin-bottom: 2rem;
}

.match-celebration {
  background: linear-gradient(135deg, #2ed573 0%, #26de81 100%);
  color: white;
}

.partial-match {
  background: linear-gradient(135deg, #ffa502 0%, #ffb732 100%);
  color: white;
}

.no-match {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.ended-section h3 {
  font-size: 2.5rem;
  margin-bottom: 0.5rem;
}

.celebration-text {
  font-size: 1.2rem;
  margin: 0;
}

.results-section {
  margin-bottom: 3rem;
}

.results-section h4 {
  font-size: 1.5rem;
  color: #495057;
  margin-bottom: 1.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 3px solid #e9ecef;
}

.results-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 1.5rem;
}

.result-movie-card {
  background: #f8f9fa;
  border-radius: 0.75rem;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s;
}

.result-movie-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.result-movie-card.common-movie {
  border: 3px solid #2ed573;
}

.result-poster {
  width: 100%;
  height: 300px;
  object-fit: cover;
}

.result-no-poster {
  width: 100%;
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e0e0e0;
  color: #999;
}

.result-info {
  padding: 1rem;
}

.result-info strong {
  display: block;
  font-size: 1rem;
  color: #333;
  margin-bottom: 0.5rem;
}

.result-meta {
  display: flex;
  gap: 0.4rem;
  flex-wrap: wrap;
}

.individual-section {
  margin-top: 2rem;
}

.participant-results {
  margin-bottom: 2.5rem;
}

.participant-name {
  font-size: 1.2rem;
  color: #495057;
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.badge-count {
  background: #667eea;
  color: white;
  padding: 0.3rem 0.8rem;
  border-radius: 1rem;
  font-size: 0.85rem;
  font-weight: 600;
}

.no-results {
  text-align: center;
  color: #adb5bd;
  padding: 2rem;
  font-style: italic;
}

.error {
  margin-top: 1rem;
  padding: 0.75rem;
  background: #fee;
  border: 1px solid #fcc;
  border-radius: 0.5rem;
  color: #c33;
  text-align: center;
}

.participants-list {
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 1px solid #e0e0e0;
}

.participants-list h4 {
  margin-bottom: 0.5rem;
  color: #555;
}

.participants-list ul {
  list-style: none;
}

.participants-list li {
  padding: 0.25rem 0;
  color: #666;
}

@media (max-width: 1024px) {
  .matching-section {
    grid-template-columns: 1fr;
  }

  .likes-sidebar {
    position: relative;
    top: 0;
    max-height: none;
  }
}

@media (max-width: 768px) {
  .movie-card {
    flex-direction: column;
  }

  .movie-poster {
    width: 100%;
  }

  .movie-poster img, .no-poster {
    width: 100%;
    max-width: 200px;
    margin: 0 auto;
    display: block;
  }

  .swipe-controls {
    gap: 1rem;
  }

  .swipe-btn {
    padding: 1rem 1.5rem;
    min-width: 100px;
  }

  .swipe-btn .swipe-icon {
    font-size: 2rem;
  }

  .results-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  }
}

.restart-section {
  margin-top: 3rem;
  padding: 2rem;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 1rem;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
}

.restart-section h4 {
  color: white;
  font-size: 1.8rem;
  margin-bottom: 1.5rem;
  text-align: center;
}

.restart-form {
  background: white;
  padding: 2rem;
  border-radius: 0.75rem;
  max-width: 600px;
  margin: 0 auto;
}

.form-row {
  margin-bottom: 1.5rem;
}

.form-row label {
  display: block;
  margin-bottom: 0.5rem;
  color: #495057;
  font-weight: 600;
}

.form-row select,
.form-row input {
  width: 100%;
  padding: 0.75rem;
  border: 2px solid #e9ecef;
  border-radius: 0.5rem;
  font-size: 1rem;
  transition: border-color 0.3s;
}

.form-row select:focus,
.form-row input:focus {
  outline: none;
  border-color: #667eea;
}

.restart-form button {
  width: 100%;
  padding: 1rem 2rem;
  background: linear-gradient(135deg, #2ed573 0%, #26d0ce 100%);
  color: white;
  border: none;
  border-radius: 0.75rem;
  font-size: 1.2rem;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(46, 213, 115, 0.3);
}

.restart-form button:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(46, 213, 115, 0.4);
}

.restart-form button:disabled {
  background: #adb5bd;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}
</style>
