import { ref } from 'vue';
import apiClient from '@/services/apiClient'; // đường dẫn tùy cấu trúc của bạn

export function useChatbot() {
    const messages = ref([]);
    const isLoading = ref(false);
    const isOnline = ref(true);
    const error = ref(null);

    const generateId = () => Date.now() + Math.random().toString(36).substr(2, 9);

    const addMessage = (text, isUser = false, isTyping = false) => {
        const message = {
            id: generateId(),
            text,
            isUser,
            isTyping,
            timestamp: Date.now(),
        };
        messages.value.push(message);
        return message;
    };

    const removeMessage = (messageId) => {
        const index = messages.value.findIndex((msg) => msg.id === messageId);
        if (index > -1) messages.value.splice(index, 1);
    };

    const clearMessages = () => {
        messages.value = [];
    };

    const sendMessage = async (message) => {
        if (!message || !message.trim()) return null;

        isLoading.value = true;
        error.value = null;

        const userMessage = addMessage(message.trim(), true);
        const typingMessage = addMessage('', false, true);

        try {
            const res = await apiClient.post('/chat/message', { message: message.trim() });

            removeMessage(typingMessage.id);

            const botMessage = addMessage(
                res.data.response || 'Xin lỗi, tôi không thể trả lời câu hỏi này.',
                false
            );

            return botMessage;
        } catch (err) {
            console.error('Error sending message:', err);
            removeMessage(typingMessage.id);

            const errorMessage = addMessage(
                'Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.',
                false
            );

            error.value = err.message || 'Lỗi không xác định';
            isOnline.value = false;
            return errorMessage;
        } finally {
            isLoading.value = false;
        }
    };

    const indexData = async () => {
        try {
            const res = await apiClient.post('/chat/index-data');
            return res.data;
        } catch (err) {
            console.error('Error indexing data:', err);
            throw err;
        }
    };

    const checkHealth = async () => {
        try {
            await apiClient.get('/chat/health');
            isOnline.value = true;
        } catch (e) {
            isOnline.value = false;
        }
    };

    const formatTime = (timestamp) =>
        new Date(timestamp).toLocaleTimeString('vi-VN', {
            hour: '2-digit',
            minute: '2-digit',
        });

    const getLastMessage = () => messages.value[messages.value.length - 1] || null;

    const getMessageById = (id) => messages.value.find((msg) => msg.id === id) || null;

    const getMessagesByType = (isUser) =>
        messages.value.filter((msg) => msg.isUser === isUser);

    const initialize = () => {
        if (messages.value.length === 0) {
            addMessage(
                'Xin chào! Tôi là trợ lý của Shoozy Shop. Tôi có thể giúp bạn tìm kiếm giày thể thao, tư vấn sản phẩm và trả lời các câu hỏi về dịch vụ của chúng tôi. Bạn cần hỗ trợ gì?',
                false
            );
        }
        checkHealth();
    };

    return {
        messages,
        isLoading,
        isOnline,
        error,
        addMessage,
        removeMessage,
        clearMessages,
        sendMessage,
        checkHealth,
        indexData,
        formatTime,
        getLastMessage,
        getMessageById,
        getMessagesByType,
        initialize,
    };
}
