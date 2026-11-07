import { texts } from '../messages/texts'
import type { MovementType } from '../types/stock'
import type { EnumDisplay } from '../types/ui'

/** Labels and badge colours for MovementType. Stock coming in is green, going out red, set aside blue. */
export const MOVEMENT_TYPE: Record<MovementType, EnumDisplay> = {
  IN: { label: texts.stockMovement.type.IN, tone: 'green' },
  OUT: { label: texts.stockMovement.type.OUT, tone: 'red' },
  ADJUST: { label: texts.stockMovement.type.ADJUST, tone: 'amber' },
  RESERVE: { label: texts.stockMovement.type.RESERVE, tone: 'blue' },
  RELEASE: { label: texts.stockMovement.type.RELEASE, tone: 'grey' },
}

/** Options for a type filter, in display order. */
export const MOVEMENT_TYPE_OPTIONS = (Object.keys(MOVEMENT_TYPE) as MovementType[]).map(type => ({
  value: type,
  label: MOVEMENT_TYPE[type].label,
}))
