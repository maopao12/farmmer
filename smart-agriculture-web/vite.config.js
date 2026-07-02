import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backendTarget = env.VITE_BACKEND_URL || 'http://localhost:8080'

  return {
    plugins: [vue()],
    resolve: {
      alias: {
        '@': resolve(__dirname, 'src')
      }
    },
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: backendTarget,
          changeOrigin: true
        },
        '/ws': {
          target: backendTarget,
          ws: true
        }
      }
    },
    // 全局变量注入
    define: {
      // sockjs-client 需要 global 对象（Vite 默认不提供）
      global: 'globalThis',
      // 生产构建时注入后端地址
      __API_BASE_URL__: JSON.stringify(env.VITE_API_BASE_URL || '')
    }
  }
})
