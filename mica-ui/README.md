# Mica administration webapp (mica-ui)

An interface to manage Mica content

## Install the dependencies

```bash
npm install
```

### Start the app in development mode (hot-code reloading, error reporting, etc.)

```bash
npm run dev
```

### Lint the files

```bash
npm run lint
```

### Format the files

```bash
npm run format
```

### Build the app for production

```bash
npm run build
```

### Customize the configuration

See [Configuring quasar.config.ts](https://quasar.dev/quasar-cli-vite/quasar-config-file).

### Run the unit tests

```bash
npm test
```

Composables, utils and small components are tested with [vitest](https://vitest.dev) and
`@vue/test-utils` (`src/**/*.test.ts`, configuration in `vitest.config.mjs`). The Maven build runs
them in the `test` phase (`-DskipTests` skips them).
