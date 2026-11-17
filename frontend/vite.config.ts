import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'

const localBackend = 'http://127.0.0.1:18082'
const defaultBackend = 'http://localhost:8080'

export default defineConfig(({ command, mode, isPreview }) => {
  const useLocalInfra = command === 'serve' && !isPreview && mode === 'local-infra'
  const backend = useLocalInfra ? localBackend : defaultBackend
  return {
    plugins: [vue(), tailwindcss()],
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('./src', import.meta.url))
      }
    },
    define: {
      'import.meta.env.VITE_LOCAL_INFRA': JSON.stringify(useLocalInfra),
      ...(useLocalInfra && { 'import.meta.env.VITE_API_BASE_URL': JSON.stringify('') })
    },
    server: {
      port: 5173,
      ...(useLocalInfra && { host: '127.0.0.1', strictPort: true }),
      proxy: {
        '/api': { target: backend, changeOrigin: true },
        '/uploads': { target: backend, changeOrigin: true },
        ...(useLocalInfra && {
          '/actuator': { target: backend, changeOrigin: true },
          '/ws': { target: backend, ws: true, changeOrigin: true }
        })
      }
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks: {
            'vendor-vue': ['vue', 'vue-router', 'pinia'],
            'vendor-element': ['element-plus', '@element-plus/icons-vue'],
            'vendor-http': ['axios'],
            'vendor-charts': ['echarts'],
            'vendor-realtime': ['@stomp/stompjs', 'sockjs-client']
          }
        }
      }
    }
  }
})
