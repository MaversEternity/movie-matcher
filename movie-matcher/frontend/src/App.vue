<template>
  <div class="app">
    <header class="header">
      <h1>🎬 Movie Matcher</h1>
      <p>Find the perfect movie with your friends!</p>
    </header>

    <main class="main">
      <CreateRoom v-if="currentView === 'create'" @room-created="handleRoomCreated" />
      <JoinRoom v-if="currentView === 'join'" :room-id="roomId" @room-joined="handleRoomJoined" />
      <Room v-if="currentView === 'room'" :room-id="roomId" :participant-id="participantId" :is-host="isHost" />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import CreateRoom from './components/CreateRoom.vue'
import JoinRoom from './components/JoinRoom.vue'
import Room from './components/Room.vue'

const currentView = ref('create')
const roomId = ref('')
const participantId = ref('')
const isHost = ref(false)

onMounted(async () => {
  // Check if URL has room ID
  const path = window.location.pathname
  const match = path.match(/\/room\/([a-f0-9-]+)/)

  if (match) {
    roomId.value = match[1]

    // Check localStorage for saved session
    const savedSession = localStorage.getItem(`room_${roomId.value}`)
    if (savedSession) {
      try {
        const session = JSON.parse(savedSession)
        participantId.value = session.participantId
        isHost.value = session.isHost

        // Verify room still exists
        const response = await fetch(`/api/rooms/${roomId.value}`)
        if (response.ok) {
          currentView.value = 'room'
          return
        }
      } catch (err) {
        console.error('Failed to restore session:', err)
      }
    }

    // No saved session or room doesn't exist, show join page
    currentView.value = 'join'
  }
})

function handleRoomCreated(data) {
  roomId.value = data.roomId
  participantId.value = data.hostId
  isHost.value = true
  currentView.value = 'room'

  // Save session
  localStorage.setItem(`room_${roomId.value}`, JSON.stringify({
    participantId: data.hostId,
    isHost: true
  }))

  // Update URL
  window.history.pushState({}, '', `/room/${data.roomId}`)
}

function handleRoomJoined(data) {
  participantId.value = data.participantId
  isHost.value = false
  currentView.value = 'room'

  // Save session
  localStorage.setItem(`room_${roomId.value}`, JSON.stringify({
    participantId: data.participantId,
    isHost: false
  }))
}
</script>

<style scoped>
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.header {
  text-align: center;
  padding: 2rem;
  color: white;
}

.header h1 {
  font-size: 3rem;
  margin-bottom: 0.5rem;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
}

.header p {
  font-size: 1.2rem;
  opacity: 0.9;
}

.main {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 2rem;
}
</style>
