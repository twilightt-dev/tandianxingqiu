export function formatPrice(value: number | string | null | undefined): string {
  if (value === null || value === undefined || (typeof value === 'string' && value.trim() === '')) {
    return '0.00'
  }

  const cents = Number(value)
  return Number.isFinite(cents) ? (cents / 100).toFixed(2) : '0.00'
}

export function formatDistance(value: number | null | undefined): string {
  if (value === null || value === undefined || !Number.isFinite(value)) {
    return ''
  }

  return value < 1000 ? `${value.toFixed(1)}m` : `${(value / 1000).toFixed(1)}km`
}
