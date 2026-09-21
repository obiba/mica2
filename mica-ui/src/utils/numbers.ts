export function toMaxDecimals(x: number | null, n: number): number | null {
  if (x === null) {
    return null;
  }
  return +x.toFixed(n);
}

/** a count with thousands separators, in the current locale */
export function formatNumber(value: number | undefined): string {
  return (value ?? 0).toLocaleString();
}
