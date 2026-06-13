import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3001,
    open: true,
    proxy: {
      // Forward all /api calls to the Spring Cloud Gateway
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      // OAuth2 — proxy directly to auth-service (8082) to avoid gateway session issues
      '/oauth2/authorize': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        secure: false,
      },
      '/oauth2/callback': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        secure: false,
      },
      '/login/oauth2': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
