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
    port: 9000,
    // Permite abrir o site pelo link do túnel da Cloudflare (iniciar-demo.ps1)
    allowedHosts: ['.trycloudflare.com'],
    // O frontend chama /api no próprio endereço e o Vite repassa ao backend. Assim um
    // único link (local ou do túnel) serve o sistema inteiro.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        configure: (proxy) => {
          // Sem o Origin do navegador, o Spring trata a chamada como de mesma origem
          // (o CORS do backend só libera http://localhost:9000)
          proxy.on('proxyReq', (req) => req.removeHeader('origin'))
        }
      }
    }
  }
})
