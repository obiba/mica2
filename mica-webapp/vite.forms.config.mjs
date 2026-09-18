// Vite build of the portal data-access form bundle (Vue 3 + Quasar + @obiba/quasar-ui-json-form),
// loaded by the data-access-*-form.ftl templates. See src/main/vue/data-access-form/main.ts.
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
import { quasar } from '@quasar/vite-plugin';
import prefixSelector from 'postcss-prefix-selector';

// Root class of the form: every Quasar / library CSS rule is scoped under it so that Quasar's
// global styles (html, body, .row, .col-*, .bg-*, .text-*...) do not leak into the Bootstrap /
// AdminLTE page. The same class is given to the Quasar portal nodes (menus, dialogs) through the
// `globalNodes` config, see main.ts.
const ROOT = '.mica-json-form';

const OUT_DIR = 'src/main/webapp/assets/js/data-access-form';

function scopeSelector(_prefix, selector, prefixedSelector) {
  const s = selector.trim();
  // our own rules are already scoped
  if (s.includes(ROOT)) return s;
  // classes that Quasar sets on <body> (dialog / scroll state): namespaced, no collision, keep them global
  if (/^(body)?\.q-body--/.test(s)) return s;
  // html, body, #q-app, :root: the resets apply to the form root instead
  if (/^(html|body|#q-app|:root)$/.test(s)) return ROOT;
  if (/^html,?\s*body$/.test(s)) return ROOT;
  // body.<class> ...: platform classes set by Quasar on <body> (desktop, mobile, platform-ios...)
  const bodyClass = s.match(/^body(\.[\w-]+(?:\.[\w-]+)*)(\s.*)?$/);
  if (bodyClass) {
    const rest = bodyClass[2] ? bodyClass[2] : '';
    return rest ? `body${bodyClass[1]} ${ROOT}${rest}` : `body${bodyClass[1]} ${ROOT}`;
  }
  return prefixedSelector;
}

// Quasar's typography on bare elements (h1-h6 at 6rem..., p, small...) would apply to the HTML of the
// `help` blocks, which is written for Bootstrap: drop those rules, the page typography applies.
const TYPOGRAPHY_ELEMENTS = /^(h[1-6]|p|small|big|sub|sup)$/;
const dropQuasarTypography = {
  postcssPlugin: 'drop-quasar-typography',
  Rule(rule) {
    const file = rule.source && rule.source.input && rule.source.input.file;
    if (file && /[\\/]quasar[\\/]dist[\\/]/.test(file) && rule.selectors.every((s) => TYPOGRAPHY_ELEMENTS.test(s.trim()))) {
      rule.remove();
    }
  },
};

export default defineConfig({
  // asset URLs relative to the CSS / JS files (the bundle is served under the assets path)
  base: './',
  // the Quasar plugin rewrites `import { X } from 'quasar'` (ours and the library's) into per-component
  // imports so that only the components in use are bundled
  plugins: [vue(), quasar({ sassVariables: false })],
  define: {
    __VUE_OPTIONS_API__: 'true',
    __VUE_PROD_DEVTOOLS__: 'false',
    __VUE_PROD_HYDRATION_MISMATCH_DETAILS__: 'false',
    __VUE_I18N_FULL_INSTALL__: 'true',
    __VUE_I18N_LEGACY_API__: 'false',
    __INTLIFY_PROD_DEVTOOLS__: 'false',
  },
  css: {
    postcss: {
      plugins: [
        dropQuasarTypography,
        prefixSelector({
          prefix: ROOT,
          transform: scopeSelector,
        }),
      ],
    },
  },
  build: {
    outDir: OUT_DIR,
    emptyOutDir: true,
    cssCodeSplit: false,
    sourcemap: false,
    // no modulepreload polyfill: the page loads the entry with <script type="module">
    modulePreload: { polyfill: false },
    // not `build.lib`: library mode inlines every asset (the icon font) as data URIs
    rollupOptions: {
      input: 'src/main/vue/data-access-form/main.ts',
      output: {
        format: 'es',
        entryFileNames: 'mica-data-access-form.js',
        assetFileNames: (asset) =>
          asset.names && asset.names.some((name) => name.endsWith('.css')) ? 'mica-data-access-form.css' : 'assets/[name][extname]',
        chunkFileNames: 'chunks/[name].js',
      },
    },
  },
});
