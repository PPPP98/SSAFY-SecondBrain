import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import { crx } from '@crxjs/vite-plugin';
import tailwindcss from '@tailwindcss/vite';
import manifest from './src/manifest.json';

export default defineConfig({
  plugins: [
    react({
      jsxRuntime: 'automatic',
    }),
    tailwindcss(),
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
      'webextension-polyfill',
      '@react-three/fiber',
      '@react-three/drei',
      'three',
    ],
    entries: [
      'src/content-scripts/overlay/index.tsx',
      'src/background/service-worker.ts',
      'src/pages/threejs-spinner/index.ts',
    ],
  },
  build: {
    // Chrome Extension 크기 제한 고려 (압축 후 ~20MB)
    chunkSizeWarningLimit: 1000,
    // 빌드 타겟 명시
    target: 'esnext',
    // Three.js 번들링 문제 해결: minify 비활성화
    minify: false,
    rollupOptions: {
      input: {
        // CRXJS가 자동으로 처리
        // threejs-spinner는 별도 빌드 필요
        'threejs-spinner': 'src/pages/threejs-spinner/index.ts',
      },
      output: {
        // Three.js를 별도 청크로 분리
        manualChunks(id) {
          if (id.includes('three')) {
            return 'three-vendor';
          }
          if (id.includes('@react-three')) {
            return 'react-three-vendor';
          }
        },
        // 읽기 쉬운 청크 이름
        chunkFileNames: 'assets/[name]-[hash].js',
        entryFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash].[ext]',
      },
    },
    // Source map 활성화 (디버깅용)
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
