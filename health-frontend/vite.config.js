import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        /** 餐图识别等长请求，避免开发时代理过早断开 */
        timeout: 180000,
      },
      '/files': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => '/api' + path,
      },
    },
  },
})
