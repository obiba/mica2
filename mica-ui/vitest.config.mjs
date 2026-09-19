// Unit tests of the composables, utils and small components (src/**/*.test.ts)
import { fileURLToPath } from 'node:url';
import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';
import { quasar } from '@quasar/vite-plugin';
import AutoImport from 'unplugin-auto-import/vite';

export default defineConfig({
  plugins: [
    vue(),
    quasar({ sassVariables: false }),
    // same auto-imports as quasar.config.ts, so that the sources under test compile unchanged
    AutoImport({
      imports: ['vue', 'vue-router', 'vue-i18n'],
      dts: false,
      dirs: ['src/stores'],
      vueTemplate: true,
    }),
  ],
  resolve: {
    alias: {
      src: fileURLToPath(new URL('./src', import.meta.url)),
      '#q-app': fileURLToPath(new URL('./test/stubs/q-app.ts', import.meta.url)),
    },
  },
  define: {
    'import.meta.env.API': JSON.stringify('/ws'),
  },
  test: {
    environment: 'jsdom',
    include: ['src/**/*.test.ts'],
    css: false,
  },
});
