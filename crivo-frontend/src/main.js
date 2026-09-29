import { createApp } from 'vue'
import { Quasar, Notify } from 'quasar'

import '@quasar/extras/material-icons/material-icons.css'
import 'quasar/src/css/index.sass'
import './css/app.scss'

import App from './App.vue'

const app = createApp(App)

app.use(Quasar, {
  plugins: { Notify },
  config: {
    notify: {
      position: 'top',
      timeout: 4000
    }
  }
})

app.mount('#app')
