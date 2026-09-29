import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { quasar, transformAssetUrls } from '@quasar/vite-plugin'

export default defineConfig({
  plugins: [
    vue({
      template: { transformAssetUrls }
    }),
    quasar({
      // Caminho absoluto com forward-slashes — necessário no Windows, onde
      // fileURLToPath retorna barras invertidas que o Sass não resolve bem.
      sassVariables: fileURLToPath(
        new URL('./src/css/quasar.variables.scss', import.meta.url)
      ).replace(/\\/g, '/')
    })
  ],
  server: {
    port: 9000
  }
})
