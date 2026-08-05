export function roundHours(value: number): number {
  return Math.round(value * 100) / 100;
}

export function formatHours(value: number | null | undefined): string {
  if (value == null || Number.isNaN(value)) {
    return '0.00h';
  }
  return `${value.toFixed(2)}h`;
}
