import { texts } from '../messages/texts'
import type { ProductStatus } from '../types/product'
import type { EnumDisplay } from '../types/ui'

/**
 * Labels and badge colours for ProductStatus, like the mockup ("Active" green, "Inactive" amber).
 * `Record<ProductStatus, ...>` makes TypeScript check that every status has an entry: a new backend status
 * added to the type is a compile error here until it gets a label.
 */
export const PRODUCT_STATUS: Record<ProductStatus, EnumDisplay> = {
  ACTIVE: { label: texts.product.status.ACTIVE, tone: 'green' },
  INACTIVE: { label: texts.product.status.INACTIVE, tone: 'amber' },
}

/** Options for a status Select or filter, in display order. */
export const PRODUCT_STATUS_OPTIONS = (Object.keys(PRODUCT_STATUS) as ProductStatus[]).map(status => ({
  value: status,
  label: PRODUCT_STATUS[status].label,
}))
