<template>
  <div class="position-fixed top-0 end-0 p-3 mt-5" style="z-index: 9999; right: 0;">
    <div
        class="toast align-items-center border-0"
        :class="toastClass"
        role="alert"
        aria-live="assertive"
        aria-atomic="true"
        ref="toastElement"
    >
      <div class="d-flex">
        <div class="toast-body text-white">
          <i :class="iconClass" class="me-2"></i>
          {{ toastMessage }}
        </div>
        <button type="button" class="btn-close btn-close-white me-2 m-auto" @click="hideToast"></button>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, computed, nextTick} from "vue";

const toastMessage = ref("");
const toastType = ref(null); // success, error, warning, info
const toastElement = ref(null);

// Computed để xác định class và icon dựa trên type
const toastClass = computed(() => {
  const classes = {
    success: 'bg-success',
    error: 'bg-danger',
    warning: 'bg-warning',
    info: 'bg-info'
  };
  return classes[toastType.value];
});

const iconClass = computed(() => {
  const icons = {
    success: 'fas fa-check-circle',
    error: 'fas fa-exclamation-triangle',
    warning: 'fas fa-exclamation-circle',
    info: 'fas fa-info-circle'
  };
  return icons[toastType.value];
});

// Expose methods để parent component có thể gọi
const showToast = async (message, type) => {
  toastMessage.value = message;
  toastType.value = type;

  if (typeof bootstrap !== 'undefined') {
    await nextTick(); // chờ DOM cập nhật xong mới tạo Toast
    const toast = new bootstrap.Toast(toastElement.value, {
      autohide: true,
      delay: 3000
    });
    toast.show();
  } else {
    console.warn('Bootstrap is not available, showing console message:', message);
  }
};

const hideToast = () => {
  if (typeof bootstrap !== 'undefined') {
    const toast = new bootstrap.Toast(toastElement.value);
    toast.hide();
  }
};

// Expose methods cho parent component
defineExpose({
  showToast,
  hideToast
});
</script>

<style scoped>
.toast {
  min-width: 300px;
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
  border-radius: 8px;
}

.toast-body {
  font-weight: 500;
  display: flex;
  align-items: center;
}

/* Animation cho toast */
.toast.show {
  animation: slideInRight 0.3s ease-out;
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}
</style>