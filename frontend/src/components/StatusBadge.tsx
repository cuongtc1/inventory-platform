import { badgeTones } from '../theme/theme'
import type { EnumDisplay } from '../types/ui'

interface StatusBadgeProps<T extends string> {
  /** The code from the API, e.g. "ACTIVE". */
  value: T
  /** Labels and tones of that enum, e.g. PRODUCT_STATUS. */
  display: Record<T, EnumDisplay>
}

/**
 * The mockup's status pill (rounded, light background, bold small text). Shows the label, never the raw code;
 * an unknown code (backend newer than the frontend) is shown as it is, in grey.
 *
 * A React component is a function that takes "props" (here: value and display) and returns what to render.
 * `<T extends string>` makes it generic, like a Java generic method: TypeScript checks that `value` is one of
 * the keys of `display`.
 */
export function StatusBadge<T extends string>({ value, display }: StatusBadgeProps<T>) {
  const option = display[value] as EnumDisplay | undefined
  const tone = badgeTones[option?.tone ?? 'grey']
  return (
    <span
      style={{
        display: 'inline-block',
        padding: '2px 10px',
        borderRadius: 20,
        fontSize: 12,
        fontWeight: 600,
        lineHeight: '20px',
        background: tone.background,
        color: tone.color,
      }}
    >
      {option?.label ?? value}
    </span>
  )
}
