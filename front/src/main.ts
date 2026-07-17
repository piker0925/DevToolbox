import {createApp} from 'vue'
import './style.css'
import App from './App.vue'
import {router} from './router'
import {BRAND} from './config/brand'
import {initAnalytics} from './config/analytics'

document.title = BRAND.siteName
initAnalytics()
createApp(App).use(router).mount('#app')
