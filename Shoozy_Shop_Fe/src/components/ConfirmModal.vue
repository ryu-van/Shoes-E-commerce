<template>
  <div v-if="show" class="modal-overlay" @click="handleOverlayClick">
    <div class="modal-container" @click.stop>
      <div class="modal-header">
        <h5 class="modal-title">
          <i :class="iconClass" class="me-2"></i>
          {{ title }}
        </h5>
        <button type="button" class="btn-close" @click="handleCancel"></button>
      </div>

      <div class="modal-body">
        <p class="modal-message">{{ message }}</p>
        <div v-if="details" class="modal-details">
          {{ details }}
        </div>
      </div>

      <div class="modal-footer">
        <button
            type="button"
            class="btn btn-secondary"
            @click="handleCancel"
            :disabled="loading"
        >
          {{ cancelText }}
        </button>
        <button
            type="button"
            class="btn"
            :class="confirmButtonClass"
            @click="handleConfirm"
            :disabled="loading"
        >
          <span v-if="loading" class="spinner-border spinner-border-sm me-2" role="status"></span>
          {{ confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: 'Xác nhận'
  },
  message: {
    type: String,
    required: true
  },
  details: {
    type: String,
    default: ''
  },
  type: {
    type: String,
    default: 'warning', // warning, danger, info, success
    validator: (value) => ['warning', 'danger', 'info', 'success'].includes(value)
  },
  confirmText: {
    type: String,
    default: 'Xác nhận'
  },
  cancelText: {
    type: String,
    default: 'Hủy'
  },
  loading: {
    type: Boolean,
    default: false
  },
  closeOnOverlay: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['confirm', 'cancel', 'close'])

const iconClass = computed(() => {
  const icons = {
    warning: 'fas fa-exclamation-triangle text-warning',
    danger: 'fas fa-exclamation-circle text-danger',
    info: 'fas fa-info-circle text-info',
    success: 'fas fa-check-circle text-success'
  }
  return icons[props.type]
})

const confirmButtonClass = computed(() => {
  const classes = {
    warning: 'btn-warning',
    danger: 'btn-danger',
    info: 'btn-info',
    success: 'btn-success'
  }
  return classes[props.type]
})

const handleConfirm = () => {
  if (!props.loading) {
    emit('confirm')
  }
}

const handleCancel = () => {
  if (!props.loading) {
    emit('cancel')
    emit('close')
  }
}

const handleOverlayClick = () => {
  if (props.closeOnOverlay && !props.loading) {
    emit('close')
  }
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.2s ease-out;
}

.modal-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
  max-width: 500px;
  width: 90%;
  max-height: 90vh;
  overflow: hidden;
  animation: slideIn 0.3s ease-out;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #e9ecef;
  background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
}

.modal-title {
  margin: 0;
  font-weight: 600;
  color: #212529;
  font-size: 1.1rem;
}

.btn-close {
  background: none;
  border: none;
  font-size: 1.2rem;
  color: #6c757d;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
}

.btn-close:hover {
  background: #e9ecef;
  color: #495057;
}

.modal-body {
  padding: 24px;
}

.modal-message {
  margin: 0 0 12px 0;
  color: #495057;
  line-height: 1.5;
  font-size: 1rem;
}

.modal-details {
  background: #f8f9fa;
  border: 1px solid #e9ecef;
  border-radius: 6px;
  padding: 12px;
  color: #6c757d;
  font-size: 0.9rem;
  line-height: 1.4;
}

.modal-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding: 20px 24px;
  border-top: 1px solid #e9ecef;
  background: #f8f9fa;
}

.modal-footer .btn {
  min-width: 100px;
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.2s;
}

.modal-footer .btn:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-20px) scale(0.95);
  }
  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* Responsive */
@media (max-width: 576px) {
  .modal-container {
    width: 95%;
    margin: 10px;
  }

  .modal-header,
  .modal-body,
  .modal-footer {
    padding: 16px;
  }

  .modal-footer {
    flex-direction: column;
  }

  .modal-footer .btn {
    width: 100%;
  }
}
</style>