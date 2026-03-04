import { defineConfig } from '#q-app/wrappers';
import AutoImport from 'unplugin-auto-import/vite';
import VueI18nPlugin from '@intlify/unplugin-vue-i18n/vite';
import checker from 'vite-plugin-checker';
import { fileURLToPath } from 'node:url';

export default defineConfig((ctx) => {
  return {
    boot: ['i18n', 'api'],

    css: ['app.scss'],

    extras: ['roboto-font', 'material-icons', 'fontawesome-v6'],

    build: {
      target: {
        browser: ['es2022', 'firefox115', 'chrome115', 'safari14'],
        node: 'node20',
      },
      typescript: {
        strict: true,
        vueShim: true,
      },
      vueRouterMode: 'hash',
      publicPath: '/mica-ui',
      env: {
        API: ctx.dev
          ? 'http://localhost:8082/ws'
          : (process.env.MICA_URL || '') + '/ws',
      },
      extendViteConf(viteConf) {
        viteConf.base = './';
      },
      vitePlugins: [
        AutoImport({
          imports: [
            'vue',
            'vue-router',
            'vue-i18n',
            'pinia',
            {
              quasar: ['useQuasar'],
            },
          ],
          dts: 'src/auto-imports.d.ts',
        }),
        VueI18nPlugin({
          langDir: fileURLToPath(new URL('./src/i18n', import.meta.url)),
          include: [
            fileURLToPath(
              new URL('./src/i18n/**', import.meta.url),
            ),
          ],
        }),
        checker({
          vueTsc: false,
          eslint: {
            lintCommand: 'eslint --max-warnings=0 "src/**/*.{ts,vue}" --ignore-pattern "src/**/*.d.ts"',
            useFlatConfig: true,
          },
        }),
      ],
    },

    devServer: {
      open: false,
    },

    framework: {
      config: {
        loadingBar: {
          color: 'primary',
          size: '3px',
          position: 'top',
        },
      },
      plugins: ['Notify', 'LocalStorage', 'LoadingBar'],
    },

    animations: [],

    ssr: {
      prodPort: 3000,
      middlewares: ['render'],
      pwa: false,
    },

    pwa: {
      workboxMode: 'GenerateSW',
    },

    cordova: {},

    capacitor: {
      hideSplashscreen: true,
    },

    electron: {
      preloadScripts: ['electron-preload'],
      openDevTools: true,
      bundler: 'packager',
    },

    bex: {
      extraScripts: [],
    },
  };
});
