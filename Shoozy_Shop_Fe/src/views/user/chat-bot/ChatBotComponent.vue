<template>
  <div class="chatbot-container">
    <!-- Chat Header -->
    <div class="chat-header">
      <div class="header-content">
        <div class="bot-avatar">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2C13.1 2 14 2.9 14 4C14 5.1 13.1 6 12 6C10.9 6 10 5.1 10 4C10 2.9 10.9 2 12 2ZM21 9V7L15 1H5C3.89 1 3 1.89 3 3V7H9V9H21ZM3 19C3 20.1 3.9 21 5 21H19C20.1 21 21 20.1 21 19V11H3V19Z" fill="currentColor"/>
          </svg>
        </div>
        <div class="bot-info">
          <h3>Hỗ trợ 24/7</h3>
          <div class="status">
            <span class="status-dot"></span>
            Đang hoạt động
          </div>
        </div>
        <button class="close-button" @click="closeChatbot">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M19 6.41L17.59 5L12 10.59L6.41 5L5 6.41L10.59 12L5 17.59L6.41 19L12 13.41L17.59 19L19 17.59L13.41 12L19 6.41Z" fill="currentColor"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- Chat Messages -->
    <div class="chat-messages" ref="messagesContainer">
      <div
          v-for="message in messages"
          :key="message.id"
          class="message"
          :class="{ 'user-message': message.isUser, 'bot-message': !message.isUser }"
      >
        <div class="message-avatar" v-if="!message.isUser">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2C13.1 2 14 2.9 14 4C14 5.1 13.1 6 12 6C10.9 6 10 5.1 10 4C10 2.9 10.9 2 12 2ZM21 9V7L15 1H5C3.89 1 3 1.89 3 3V7H9V9H21ZM3 19C3 20.1 3.9 21 5 21H19C20.1 21 21 20.1 21 19V11H3V19Z" fill="currentColor"/>
          </svg>
        </div>

        <div class="message-content">
          <div class="message-bubble">
            <!-- Typing Indicator -->
            <div v-if="message.isTyping" class="typing-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>

            <!-- Regular Text Message -->
            <div v-else-if="!message.data || message.type === 'text'"
                 class="text-content"
                 v-html="formatMessage(message.text)">
            </div>

            <!-- Product Results -->
            <div v-else-if="message.type === 'product'" class="product-results">
              <div class="response-header" v-html="formatMessage(message.text)"></div>
              <div class="products-list">
                <div v-for="product in message.data" :key="product.productId" class="product-item">
                  <div class="product-thumbnail">
                    <img :src="product.thumbnail" width="48" height="48" alt="Product Image" />
                  </div>
                  <div class="product-details">
                    <div class="product-name">{{ product.name }}</div>
                    <div class="product-price">{{ formatPrice(product.minPrice, product.maxPrice) }}</div>
                    <router-link :to="`/product-detail/${product.productId}`" class="btn detail-btn">
                      XEM CHI TIẾT
                    </router-link>
                  </div>
                </div>
              </div>
            </div>

            <!-- Promotion Results -->
            <div v-else-if="message.type === 'promotion'" class="promotion-results">
              <div class="response-header" v-html="formatMessage(message.text)"></div>
              <div class="promotions-list">
                <div v-for="promotion in message.data" :key="promotion.id" class="promotion-item">
                  <h4>{{ promotion.title }}</h4>
                  <p>{{ promotion.description }}</p>
                  <div class="promotion-meta">
                    <span v-if="promotion.discount">Giảm {{ promotion.discount }}%</span>
                    <span v-if="promotion.validUntil">Đến: {{ formatDate(promotion.validUntil) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Quick Actions trong vùng chat -->
      <div class="chat-quick-actions" v-if="showQuickActions">
        <button
            v-for="action in quickActions"
            :key="action.id"
            @click="sendQuickMessage(action.message)"
            class="chat-quick-btn"
        >
          {{ action.label }}
        </button>
      </div>
    </div>

    <!-- Chat Input -->
    <div class="chat-input">
      <form @submit.prevent="sendMessage" class="input-form">
        <input
            v-model="currentMessage"
            type="text"
            placeholder="Nhập tin nhắn..."
            class="message-input"
            :disabled="isLoading"
        />
        <button
            type="submit"
            class="send-button"
            :disabled="!currentMessage.trim() || isLoading"
        >
          <svg v-if="!isLoading" width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M2.01 21L23 12L2.01 3L2 10L17 12L2 14L2.01 21Z" fill="currentColor"/>
          </svg>
          <svg v-else class="loading" width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12,4V2A10,10 0 0,0 2,12H4A8,8 0 0,1 12,4Z" fill="currentColor"/>
          </svg>
        </button>
      </form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'

// Emits
const emit = defineEmits(['close'])

// Props
const props = defineProps({
  apiBaseUrl: {
    type: String,
    default: 'http://localhost:8080/api/v1'
  }
})

// Reactive data
const messages = ref([])
const currentMessage = ref('')
const isLoading = ref(false)
const messagesContainer = ref(null)
const showQuickActions = ref(true)

// Quick actions
const quickActions = ref([
  { id: 1, label: 'Giày Nike', message: 'Tìm giày Nike' },
  { id: 2, label: 'Giày dưới 2 triệu', message: 'Giày dưới 2 triệu' },
  { id: 3, label: 'Chính sách đổi trả', message: 'Chính sách đổi trả như thế nào?' },
  { id: 4, label: 'Khuyến mãi', message: 'Có chương trình khuyến mãi nào không?' }
])

// Methods
const generateId = () => {
  return Date.now() + Math.random().toString(36).substr(2, 9)
}

const formatTime = (timestamp) => {
  return new Date(timestamp).toLocaleTimeString('vi-VN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatDate = (dateString) => {
  return new Date(dateString).toLocaleDateString('vi-VN')
}

const formatPrice = (minPrice, maxPrice) => {
  if (minPrice === maxPrice) {
    return `${minPrice.toLocaleString('vi-VN')}đ`
  }
  return `${minPrice.toLocaleString('vi-VN')}đ - ${maxPrice.toLocaleString('vi-VN')}đ`
}

const formatMessage = (text) => {
  if (!text) return ''
  return text.replace(/\n/g, '<br>').replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
}

const scrollToBottom = async () => {
  await nextTick()
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const addMessage = (text = '', isUser = false, isTyping = false, type = 'text', data = null, totalCount = null) => {
  const message = {
    id: generateId(),
    text,
    isUser,
    isTyping,
    type,
    data,
    totalCount,
    timestamp: Date.now()
  }
  messages.value.push(message)
  scrollToBottom()
  return message
}

const closeChatbot = () => {
  emit('close')
}

const sendMessage = async () => {
  const message = currentMessage.value.trim()
  if (!message || isLoading.value) return

  showQuickActions.value = false
  addMessage(message, true)
  currentMessage.value = ''

  isLoading.value = true
  const typingMessage = addMessage('', false, true)

  try {
    const response = await fetch(`${props.apiBaseUrl}/chat/message`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ message })
    })

    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }

    const aiResponse = await response.json()

    const typingIndex = messages.value.findIndex(msg => msg.id === typingMessage.id)
    if (typingIndex > -1) {
      messages.value.splice(typingIndex, 1)
    }

    const botMessage = aiResponse.message || 'Xin lỗi, tôi không thể trả lời câu hỏi này.'
    addMessage(
        botMessage,
        false,
        false,
        aiResponse.type || 'text',
        aiResponse.data || null,
        aiResponse.totalCount || null
    )
    console.log('AI Response:', aiResponse)

  } catch (error) {
    console.error('Error sending message:', error)

    const typingIndex = messages.value.findIndex(msg => msg.id === typingMessage.id)
    if (typingIndex > -1) {
      messages.value.splice(typingIndex, 1)
    }

    addMessage('❌ Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.', false)
  } finally {
    isLoading.value = false
  }
}

const sendQuickMessage = (message) => {
  currentMessage.value = message
  sendMessage()
}

// Lifecycle
onMounted(() => {
  addMessage(
      'Shoozy shop xin chào quý khách ! chúng tôi có thể giúp gì cho bạn ?',
      false,
      false,
      'text'
  )
})

// Watch for messages changes to auto-scroll
watch(messages, () => {
  scrollToBottom()
}, { deep: true })
</script>

<style scoped>
.chatbot-container {
  display: flex;
  flex-direction: column;
  height: 600px;
  max-width: 400px;
  margin: 0 auto;
  background: #f5f5f5;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.chat-header {
  background: #4a4a4a;
  color: white;
  padding: 16px;
}

.header-content {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
}

.bot-avatar {
  width: 36px;
  height: 36px;
  background: #5a5a5a;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bot-info {
  flex: 1;
}

.bot-info h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.status {
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
}

.status-dot {
  width: 8px;
  height: 8px;
  background: #4ade80;
  border-radius: 50%;
}

.close-button {
  background: none;
  border: none;
  color: white;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-button:hover {
  background: rgba(255, 255, 255, 0.1);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f5f5;
}

.message {
  display: flex;
  margin-bottom: 12px;
  align-items: flex-start;
}

.user-message {
  justify-content: flex-end;
}

.bot-message {
  justify-content: flex-start;
}

.message-avatar {
  width: 28px;
  height: 28px;
  background: #4a4a4a;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  margin-right: 8px;
  flex-shrink: 0;
}

.message-content {
  max-width: 75%;
}

.message-bubble {
  padding: 12px 16px;
  border-radius: 18px;
  word-wrap: break-word;
}

.user-message .message-bubble {
  background: #333;
  color: white;
  border-bottom-right-radius: 4px;
}

.bot-message .message-bubble {
  background: white;
  color: #333;
  border-bottom-left-radius: 4px;
}

.text-content {
  line-height: 1.5;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  align-items: center;
}

.typing-indicator span {
  width: 6px;
  height: 6px;
  background: #999;
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

/* Product Results */
.product-results {
  max-width: none;
}

.response-header {
  margin-bottom: 12px;
  font-weight: 500;
}

.products-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.product-item {
  display: flex;
  gap: 12px;
  align-items: center;
  background: #f9f9f9;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 12px;
}

.product-thumbnail {
  width: 48px;
  height: 48px;
  background: #e8e8e8;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.product-details {
  flex: 1;
}

.product-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin: 0 0 4px 0;
}

.product-price {
  font-size: 13px;
  color: #666;
  margin: 0 0 8px 0;
}

.detail-btn {
  background: #dbeafe;
  color: #2563eb;
  border: none;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 500;
  cursor: pointer;
  text-transform: uppercase;
  text-decoration: none;
  display: inline-block;
}

.detail-btn:hover {
  background: #bfdbfe;
}

/* Promotion Results */
.promotion-results {
  max-width: none;
}

.promotions-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.promotion-item {
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 6px;
  padding: 12px;
}

.promotion-item h4 {
  margin: 0 0 6px 0;
  font-size: 14px;
  color: #856404;
}

.promotion-item p {
  margin: 0 0 6px 0;
  font-size: 12px;
  color: #856404;
}

.promotion-meta {
  display: flex;
  gap: 8px;
  font-size: 11px;
}

.promotion-meta span {
  background: #ffeaa7;
  color: #856404;
  padding: 2px 6px;
  border-radius: 3px;
}

/* Quick Actions trong vùng chat */
.chat-quick-actions {
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
  align-items: flex-start;
  margin-left: 36px;
}

.chat-quick-btn {
  padding: 8px 16px;
  background: #e8e8e8;
  border: none;
  border-radius: 16px;
  font-size: 14px;
  color: #555;
  cursor: pointer;
  text-align: left;
  font-weight: normal;
  transition: all 0.2s ease;
}

.chat-quick-btn:hover {
  background: #d8d8d8;
}

.chat-input {
  padding: 16px;
  border-top: 1px solid #e0e0e0;
  background: white;
}

.input-form {
  display: flex;
  gap: 8px;
  align-items: center;
}

.message-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #d0d0d0;
  border-radius: 20px;
  outline: none;
  font-size: 14px;
}

.message-input:focus {
  border-color: #4a4a4a;
}

.message-input:disabled {
  background: #f5f5f5;
  color: #999;
}

.send-button {
  width: 40px;
  height: 40px;
  background: #4a4a4a;
  border: none;
  border-radius: 50%;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-button:hover:not(:disabled) {
  background: #3a3a3a;
}

.send-button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.loading {
  animation: spin 1s linear infinite;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-8px);
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.chat-messages::-webkit-scrollbar {
  width: 4px;
}

.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 2px;
}

@media (max-width: 480px) {
  .chatbot-container {
    height: 100vh;
    max-width: 100%;
    border-radius: 0;
  }

  .message-content {
    max-width: 85%;
  }

  .chat-quick-actions {
    margin-left: 36px;
  }
}
</style>