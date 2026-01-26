<template>
  <div class="join-room">
    <div class="card">
      <h2>Join Movie Matching Room</h2>

      <div v-if="loading" class="loading">Loading room info...</div>

      <div v-else-if="roomInfo">
        <div class="room-info">
          <h3>Room Details</h3>
          <p><strong>Participants:</strong> {{ roomInfo.participants_count }}</p>
          <p v-if="roomInfo.filters.genre"><strong>Genre:</strong> {{ roomInfo.filters.genre }}</p>
          <p v-if="roomInfo.filters.year_from || roomInfo.filters.year_to">
            <strong>Years:</strong>
            {{ roomInfo.filters.year_from || '?' }} - {{ roomInfo.filters.year_to || '?' }}
          </p>
        </div>

        <form @submit.prevent="joinRoom">
          <div class="form-group">
            <label for="participantId">Your Name</label>
            <input
              id="participantId"
              v-model="participantId"
              type="text"
              placeholder="Enter your name"
              required
            />
          </div>

          <button type="submit" class="btn btn-primary" :disabled="joining">
            {{ joining ? 'Joining...' : 'Join Room' }}
          </button>
        </form>
      </div>

      <div v-if="error" class="error">{{ error }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const props = defineProps({
  roomId: String
})

const emit = defineEmits(['room-joined'])

const participantId = ref('')
const roomInfo = ref(null)
const loading = ref(true)
const joining = ref(false)
const error = ref('')

onMounted(async () => {
  await fetchRoomInfo()
})

async function fetchRoomInfo() {
  loading.value = true
  error.value = ''

  try {
    const response = await fetch(`/api/rooms/${props.roomId}`)

    if (!response.ok) {
      throw new Error('Room not found')
    }

    roomInfo.value = await response.json()
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}

async function joinRoom() {
  joining.value = true
  error.value = ''

  try {
    const response = await fetch(`/api/rooms/${props.roomId}/join`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        participant_id: participantId.value
      })
    })

    if (!response.ok) {
      throw new Error('Failed to join room')
    }

    const data = await response.json()

    if (data.success) {
      emit('room-joined', {
        participantId: participantId.value
      })
    } else {
      throw new Error('Could not join room')
    }
  } catch (err) {
    error.value = err.message
  } finally {
    joining.value = false
  }
}
</script>

<style scoped>
.join-room {
  width: 100%;
  max-width: 500px;
}

.card {
  background: white;
  border-radius: 1rem;
  padding: 2rem;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

h2 {
  margin-bottom: 1.5rem;
  color: #667eea;
  text-align: center;
}

h3 {
  margin-bottom: 1rem;
  color: #555;
}

.loading {
  text-align: center;
  padding: 2rem;
  color: #888;
}

.room-info {
  background: #f8f9fa;
  padding: 1.5rem;
  border-radius: 0.5rem;
  margin-bottom: 1.5rem;
}

.room-info p {
  margin: 0.5rem 0;
  color: #555;
}

.form-group {
  margin-bottom: 1.5rem;
}

label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #555;
}

input {
  width: 100%;
  padding: 0.75rem;
  border: 2px solid #e0e0e0;
  border-radius: 0.5rem;
  font-size: 1rem;
  transition: border-color 0.3s;
}

input:focus {
  outline: none;
  border-color: #667eea;
}

.btn {
  width: 100%;
  padding: 1rem;
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

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
</style>
