<template>
  <div class="modal-overlay" @click="$emit('close')">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3>Chọn sản phẩm muốn trả hàng</h3>
        <button class="close-btn" @click="$emit('close')">&times;</button>
      </div>

      <div class="modal-body">
        <div
          v-if="returnableItems.length === 0"
          class="text-muted"
          style="padding: 12px; font-style: italic;"
        >
          Không còn sản phẩm nào có thể trả hàng.
        </div>

        <div
          v-else
          v-for="item in returnableItems"
          :key="item.orderDetailId"
          class="return-item"
        >
          <input
            type="checkbox"
            :id="`item-${item.orderDetailId}`"
            :value="item"
            v-model="selectedItems"
          />
         <label :for="`item-${item.orderDetailId}`" class="item-label">
            <div class="thumb-wrapper">
              <img :src="item.thumbnail" alt="" class="thumb" />
              <span class="quantity-badge">x{{ item.quantity }}</span>
            </div>
            <span class="name">
              {{ item.productName }} - {{ item.color }} / Size {{ item.size }}
            </span>
          </label>

        </div>
      </div>

      <div class="modal-footer">
        <button class="btn btn-outline" @click="$emit('close')">Hủy</button>
        <button
          class="btn btn-primary"
          :disabled="selectedItems.length === 0"
          @click="proceedToReturn"
        >
          Tiếp tục
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { getReturnableItems } from '@/service/OrderApi.js'

const props = defineProps({
  orderId: [Number, String],
  userId: [Number, String],
  visible: Boolean,
  items: { type: Array, default: () => [] } 
});
const emit = defineEmits(['close', 'proceed'])

const selectedItems = ref([])
const returnableItems = ref([])

watch(
  () => props.visible,
  async (visible) => {
    if (!visible) {
      selectedItems.value = []
      return
    }

    try {
      const res = await getReturnableItems(props.orderId, props.userId)
      console.log('🎯 Returnable items:', res.data.data)
      returnableItems.value = res.data.data
    } catch (error) {
      console.error('❌ Lỗi khi tải returnable items:', error)
      returnableItems.value = []
    }
  },
  { immediate: true }
)

const proceedToReturn = () => {
  emit('proceed', selectedItems.value)
}
</script>

<style scoped>
.item-label {
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
}

.thumb-wrapper {
  position: relative;
  display: inline-block;
}

.quantity-badge {
  position: absolute;
  bottom: 4px;
  right: 4px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 10px;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.modal-content {
  background: white;
  padding: 20px;
  border-radius: 8px;
  width: 600px;
  max-height: 90vh;
  overflow-y: auto;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.modal-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.return-item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.return-item img.thumb {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #ccc;
}
.return-item .name {
  font-size: 14px;
}
.modal-footer {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.btn {
  padding: 6px 14px;
  border-radius: 4px;
  font-weight: 500;
}
.btn-primary {
  background-color: #007bff;
  color: white;
  border: none;
}
.btn-outline {
  background: white;
  border: 1px solid #ccc;
  color: #333;
}
.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
}
</style>
