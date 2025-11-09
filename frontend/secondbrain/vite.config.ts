import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import { tanstackRouter } from '@tanstack/router-plugin/vite';
import path from 'path';
import svgr from 'vite-plugin-svgr';
import { crx } from '@crxjs/vite-plugin';
// eslint-disable-next-line no-relative-import-paths/no-relative-import-paths
import manifest from './src/manifest.json';

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    tanstackRouter({
      target: 'react',
      routesDirectory: './src/routes',
      generatedRouteTree: './src/routeTree.gen.ts',
      autoCodeSplitting: true,
    }),
    react(),
    svgr(),
    crx({ manifest }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
  define: {
    // Vue feature flags for @milkdown/crepe compatibility
    __VUE_OPTIONS_API__: 'true',
    __VUE_PROD_DEVTOOLS__: 'false',
    __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: 'false',
  },
  // Extension을 위한 최적화
  optimizeDeps: {
    include: ['react', 'react-dom'],
    exclude: [],
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks(id) {
          // React를 단일 청크로 관리
          if (id.includes('node_modules/react')) {
            return 'react-vendor';
          }
        },
      },
    },
  },
});
