import { BUSINESS_TIME_ZONE, CURRENCY } from '../constants/app'

/**
 * Turning API values into text for people (docs/conventions.md section 10). The same formats as the backend's
 * DateTimeUtils and MoneyUtils. Use these everywhere; never toLocaleString() without a time zone, which would use
 * the settings of the viewer's computer.
 *
 * `Intl` is built into every browser (and Node), so no date or number library is needed.
 */

/** Shown for a missing value (null, undefined, empty string). */
export const EMPTY_VALUE = '-'

const dateParts = new Intl.DateTimeFormat('en-GB', {
  timeZone: BUSINESS_TIME_ZONE,
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
  hourCycle: 'h23',
})

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: CURRENCY })

/** "2026-10-21T18:30:00Z" → "22/10/2026" (the date in Vietnam). */
export function formatDate(iso: string | null | undefined): string {
  const parts = vietnamParts(iso)
  return parts ? `${parts.day}/${parts.month}/${parts.year}` : EMPTY_VALUE
}

/** "2026-10-21T18:30:00Z" → "22/10/2026 01:30" (Vietnam time, 24 hours). */
export function formatDateTime(iso: string | null | undefined): string {
  const parts = vietnamParts(iso)
  return parts ? `${parts.day}/${parts.month}/${parts.year} ${parts.hour}:${parts.minute}` : EMPTY_VALUE
}

/** 1234.5 → "$1,234.50". */
export function formatMoney(amount: number | null | undefined): string {
  return amount === null || amount === undefined || Number.isNaN(amount) ? EMPTY_VALUE : money.format(amount)
}

/**
 * A date picked in the UI, for a date filter: Date → "YYYY-MM-DD" as that day in Vietnam (the backend's `from` /
 * `to` parameters, docs/conventions.md section 4). Ant Design's DatePicker gives a dayjs value: pass `.toDate()`.
 */
export function toApiDate(date: Date): string {
  const parts = dateParts.formatToParts(date)
  const part = (type: Intl.DateTimeFormatPartTypes) => parts.find(p => p.type === type)?.value ?? ''
  return `${part('year')}-${part('month')}-${part('day')}`
}

/**
 * The pieces of a moment as seen in Vietnam. formatToParts() instead of format(): the exact output of format()
 * differs between browsers ("22/10/2026, 01:30" vs "22/10/2026 01:30"), the parts don't.
 */
function vietnamParts(iso: string | null | undefined) {
  if (!iso) {
    return null
  }
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) {
    return null
  }
  const parts = dateParts.formatToParts(date)
  const part = (type: Intl.DateTimeFormatPartTypes) => parts.find(p => p.type === type)?.value ?? ''
  return { day: part('day'), month: part('month'), year: part('year'), hour: part('hour'), minute: part('minute') }
}
