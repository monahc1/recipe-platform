import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react'
import fs from 'fs';

export default defineConfig({
  server: {
    port: 5173,
    https: {
      key: fs.readFileSync('./localhost-key.pem'),
      cert: fs.readFileSync('./localhost.pem'),
    }
  }
});
