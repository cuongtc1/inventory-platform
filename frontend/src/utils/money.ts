/**
 * An estimate of an order's total while the form is filled in (docs/conventions.md section 10, agreed in T5.5).
 *
 * JavaScript numbers are binary floating point: 0.1 + 0.2 is 0.30000000000000004. So amounts are turned into whole
 * cents first (12.00 → 1200), and cents are multiplied and added as integers, which is exact up to 2^53 (about
 * $90 trillion). The saved order's total always comes from the backend, which uses BigDecimal.
 */

/** 12.5 → 1250. Math.round removes the tiny error of the multiplication: 0.29 * 100 is 28.999999999999996. */
export function toCents(amount: number): number {
  return Math.round(amount * 100)
}

/** Unit price × quantity, in cents. */
export function lineTotalCents(unitPrice: number, quantity: number): number {
  return toCents(unitPrice) * quantity
}

/** 10800 → 108, for formatMoney. Exact enough for display: the true value has 2 decimals. */
export function fromCents(cents: number): number {
  return cents / 100
}
