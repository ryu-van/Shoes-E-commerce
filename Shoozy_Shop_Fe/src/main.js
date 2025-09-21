import { createApp } from 'vue'
import App from './App.vue'
import { createPinia } from 'pinia'
import 'bootstrap/dist/css/bootstrap.css'
import * as bootstrap from 'bootstrap'
import 'bootstrap/dist/js/bootstrap.bundle.js'
import router from './router'

const app = createApp(App)

const pinia = createPinia()
app.use(pinia)
app.use(router)
app.mount('#app')
window.bootstrap = bootstrap