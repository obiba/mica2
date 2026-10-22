// jsdom has no ResizeObserver, which quasar's resize composable relies on
globalThis.ResizeObserver ??= class {
  observe() {}
  unobserve() {}
  disconnect() {}
};
