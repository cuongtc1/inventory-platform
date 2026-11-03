/**
 * App-wide settings that match the backend's rules (docs/conventions.md).
 * `as const` makes TypeScript treat the values as fixed literals, not just "string" or "number".
 */

/** The business works in Vietnam time; dates are shown in this zone (backend: DateTimeUtils.BUSINESS_ZONE). */
export const BUSINESS_TIME_ZONE = 'Asia/Ho_Chi_Minh'

/** One currency, like the backend's MoneyUtils. */
export const CURRENCY = 'USD'

/**
 * Below this width (Ant Design's "lg" breakpoint) the layout switches to mobile: the sidebar becomes a slide-in
 * menu and paddings shrink. Phones and portrait tablets are below it; laptops and desktops above.
 */
export const MOBILE_MAX_WIDTH = 991

/** Sidebar widths from the mockup: 236 px open, 70 px collapsed (icons only). */
export const SIDEBAR_WIDTH = 236
export const SIDEBAR_COLLAPSED_WIDTH = 70

/** Paging, like docs/conventions.md section 4: default 20, the backend never returns more than 100. */
export const DEFAULT_PAGE_SIZE = 20
export const MAX_PAGE_SIZE = 100
export const PAGE_SIZE_OPTIONS = [10, 20, 50, 100] as const

/** A form card is at most this wide, so its two columns stay readable on big screens (pages use the full width). */
export const FORM_MAX_WIDTH = 960
