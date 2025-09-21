<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-100" style="margin-top: 350px">
    <div class="max-w-md mx-auto p-6 bg-white rounded-lg shadow-md text-center">
      <h2 class="text-2xl font-bold mb-4 text-red-600">Không có quyền truy cập</h2>
      <p class="mb-4">Bạn không có quyền truy cập trang này.</p>
      <router-link
          to="/"
          class="inline-block bg-blue-500 text-white p-2 rounded hover:bg-blue-600"
      >
        Quay về trang chủ
      </router-link>
    </div>
  </div>
</template>

<script setup>
</script>