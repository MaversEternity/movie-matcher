<template>
  <div class="create-room">
    <div class="card">
      <h2>Create a Movie Matching Room</h2>

      <form @submit.prevent="createRoom">
        <div class="form-group">
          <label for="hostId">Your Name</label>
          <input
            id="hostId"
            v-model="hostId"
            type="text"
            placeholder="Enter your name"
            required
          />
        </div>

        <div class="form-group">
          <label for="genre">Genre (optional)</label>
          <select id="genre" v-model="filters.genre">
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

        <div class="form-group">
          <label for="yearFrom">Year From (optional)</label>
          <input
            id="yearFrom"
            v-model.number="filters.year_from"
            type="number"
            min="1900"
            max="2030"
            placeholder="e.g., 2000"
          />
        </div>

        <div class="form-group">
          <label for="yearTo">Year To (optional)</label>
          <input
            id="yearTo"
            v-model.number="filters.year_to"
            type="number"
            min="1900"
            max="2030"
            placeholder="e.g., 2024"
          />
        </div>

        <button type="submit" class="btn btn-primary" :disabled="loading">
          {{ loading ? 'Creating...' : 'Create Room' }}
        </button>
      </form>

      <div v-if="error" class="error">{{ error }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['room-created'])

const hostId = ref('')
const filters = ref({
  genre: '',
  year_from: null,
  year_to: null
})
const loading = ref(false)
const error = ref('')

async function createRoom() {
  loading.value = true
  error.value = ''

  try {
    const response = await fetch('/api/rooms', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        host_id: hostId.value,
        filters: {
          genre: filters.value.genre || null,
          year_from: filters.value.year_from || null,
          year_to: filters.value.year_to || null,
        }
      })
    })

    if (!response.ok) {
      throw new Error('Failed to create room')
    }

    const data = await response.json()
    emit('room-created', {
      roomId: data.room_id,
      hostId: hostId.value
    })
  } catch (err) {
    error.value = err.message
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.create-room {
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

.form-group {
  margin-bottom: 1.5rem;
}

label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 600;
  color: #555;
}

input, select {
  width: 100%;
  padding: 0.75rem;
  border: 2px solid #e0e0e0;
  border-radius: 0.5rem;
  font-size: 1rem;
  transition: border-color 0.3s;
}

input:focus, select:focus {
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
