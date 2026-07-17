import {createApp} from 'vue'
import './style.css'
import App from './App.vue'
import {router} from './router'
import {BRAND} from './config/brand'

document.title = BRAND.siteName
createApp(App).use(router).mount('#app')
