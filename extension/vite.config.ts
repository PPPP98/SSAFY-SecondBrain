import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import { crx } from '@crxjs/vite-plugin';
import manifest from './src/manifest.json';

export default defineConfig({
  plugins: [
    react({
      jsxRuntime: 'automatic',
    }),
    crx({
      manifest,
      contentScripts: {
        injectCss: true,
      },
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  optimizeDeps: {
    include: [
      'react',
      'react-dom',
      'react/jsx-runtime',
      'react/jsx-dev-runtime',
      'react-dom/client',
    ],
    entries: ['src/content-scripts/overlay/index.tsx', 'src/background/service-worker.ts'],
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: undefined,
      },
    },
    sourcemap: true,
    modulePreload: false,
    cssCodeSplit: false,
  },
  server: {
    port: 5174,
    strictPort: true,
    hmr: {
      host: 'localhost',
      protocol: 'ws',
      port: 5174,
    },
  },
});
