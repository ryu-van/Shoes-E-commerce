<script setup>
import { connectWebSocket, addMessageListener } from '@/service/Websocket.js'
import emitter from '@/service/EvenBus.js'
import {onMounted} from "vue";

onMounted(() => {
  connectWebSocket()

  addMessageListener((data) => {
    emitter.emit(data.type, data)
  })
})
</script>

<template>
  <router-view></router-view>
</template>

<style scoped></style>