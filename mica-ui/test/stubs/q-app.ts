// Stand-in for the Quasar CLI virtual module `#q-app` used by the boot files.
export function defineBoot<T>(fn: T): T {
  return fn;
}
