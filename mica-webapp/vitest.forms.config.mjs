// Unit tests of the data access form bundle glue (src/main/vue/data-access-form/*.test.ts)
import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';
import { quasar } from '@quasar/vite-plugin';

export default defineConfig({
  plugins: [vue(), quasar({ sassVariables: false })],
  test: {
    environment: 'jsdom',
    include: ['src/main/vue/**/*.test.ts'],
    css: false,
  },
});
