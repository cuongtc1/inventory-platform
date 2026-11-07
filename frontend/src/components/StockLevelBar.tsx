import { texts } from '../messages/texts'
import { colors } from '../theme/theme'
import type { StockLevelResponse } from '../types/stock'

interface StockLevelBarProps {
  level: StockLevelResponse
}

/**
 * The mockup's Level column (`.pb`): a thin bar, red when low. The bar is full at twice the reorder level, so
 * halfway is the point where a product becomes low stock.
 */
export function StockLevelBar({ level }: StockLevelBarProps) {
  const percent = fillPercent(level.available, level.reorderLevel)

  return (
    <div
      role="progressbar"
      aria-label={texts.stockLevel.fields.level}
      aria-valuenow={percent}
      aria-valuemin={0}
      aria-valuemax={100}
      style={{ height: 6, minWidth: 90, borderRadius: 4, background: colors.progressTrack }}
    >
      <div
        style={{
          width: `${percent}%`,
          height: '100%',
          borderRadius: 4,
          background: level.lowStock ? colors.red : colors.primary,
        }}
      />
    </div>
  )
}

/** For display only. Without a reorder level there is no scale: full when anything is available, else empty. */
function fillPercent(available: number, reorderLevel: number): number {
  if (available <= 0) {
    return 0
  }
  return reorderLevel === 0 ? 100 : Math.min(100, Math.round((available / (reorderLevel * 2)) * 100))
}
