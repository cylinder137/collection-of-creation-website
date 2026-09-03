import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    host: true,
    // 允许通过任意域名/Host 访问（花生壳/Cloudflare 等内网穿透域名），方便外网联调
    allowedHosts: true,
    // 后端接口代理：开发环境把 /api 转发到 Spring Boot（默认 8080）
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
  // 反爬/加固：生产构建剔除调试痕迹（console.log/debug、debugger 语句）
  esbuild: {
    pure: ['console.log', 'console.debug'],
    drop: ['debugger'],
  },
})
