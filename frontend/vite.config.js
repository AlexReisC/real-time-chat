import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      // Todos os endpoints REST passam pelo prefixo /api/v1
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // WebSocket upgrade para o Chat Service via Gateway
      '/ws': {
        target: 'ws://localhost:8080',
        ws: true,
        changeOrigin: true,
      },
    },
  },
});
