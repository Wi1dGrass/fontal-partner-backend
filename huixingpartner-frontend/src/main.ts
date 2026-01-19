import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import './style.css'
import './styles/vant-theme.scss'
import App from './App.vue'
import router from './router'

const app = createApp(App)
const pinia = createPinia()

// 配置 Pinia 数据持久化
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
app.use(router)
app.mount('#app')
