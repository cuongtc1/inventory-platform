import type { BadgeTone } from '../theme/theme'

/**
 * How one value of a backend enum is shown: a label for people and a badge tone. The API sends only the code
 * ("ACTIVE"); every enum gets one `Record<Enum, EnumDisplay>` in constants/ (e.g. PRODUCT_STATUS).
 */
export interface EnumDisplay {
  label: string
  tone: BadgeTone
}
