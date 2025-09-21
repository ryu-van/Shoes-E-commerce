<script setup>
import SideBarComponent from "@/layouts/admin/components/SideBarComponent.vue";
import NavbarComponent from "@/layouts/admin/components/NavbarComponent.vue";
import { onMounted } from 'vue'

// Hàm load script từ thư mục /public/vendor/
function loadScript(src) {
  return new Promise((resolve, reject) => {
    const script = document.createElement("script")
    script.src = src
    script.async = true
    script.onload = resolve
    script.onerror = reject
    document.body.appendChild(script)
  })
}

// Hàm load CSS từ thư mục /public/vendor/
function loadStyle(href) {
  const link = document.createElement("link")
  link.rel = "stylesheet"
  link.href = href
  document.head.appendChild(link)
}

onMounted(async () => {
  // Load CSS admin
  loadStyle('/vendor/sweetalert2/dist/sweetalert2.min.css')
  loadStyle('/vendor/notyf/notyf.min.css')
  loadStyle('/vendor/bootstrap/dist/css/bootstrap.min.css')
  loadStyle('/src/css/volt.css') // Nếu volt.css cũng nằm trong public, dùng /volt.css

  // Load JS admin
  await loadScript('/vendor/@popperjs/core/dist/umd/popper.min.js')
  await loadScript('/vendor/bootstrap/dist/js/bootstrap.min.js')
  await loadScript('/vendor/onscreen/dist/on-screen.umd.min.js')
  await loadScript('/vendor/nouislider/dist/nouislider.min.js')
  await loadScript('/vendor/smooth-scroll/dist/smooth-scroll.polyfills.min.js')
  await loadScript('/vendor/chartist/dist/chartist.min.js')
  await loadScript('/vendor/chartist-plugin-tooltips/dist/chartist-plugin-tooltip.min.js')
  await loadScript('/vendor/sweetalert2/dist/sweetalert2.all.min.js')
  await loadScript('/vendor/vanillajs-datepicker/dist/js/datepicker.min.js')
  await loadScript('/vendor/notyf/notyf.min.js')
  await loadScript('/vendor/simplebar/dist/simplebar.min.js')
})
</script>

<template>
  <main class="content">
    <NavbarComponent/>
    <SideBarComponent/>
    <main class="p-4">
      <router-view />
    </main>
  </main>
</template>

<style scoped>
* {
  background-color: white !important;
}
</style>