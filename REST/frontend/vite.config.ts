import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// `npm run build` writes the production bundle into Spring Boot's static folder,
// so the API jar serves the UI at http://localhost:8080/.
// `npm run dev` serves the UI at http://localhost:5173/ and proxies /api to Spring Boot.
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
  },
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
