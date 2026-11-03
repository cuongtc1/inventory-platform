import { useSyncExternalStore } from 'react'
import { MOBILE_MAX_WIDTH } from '../constants/app'

const query = `(max-width: ${MOBILE_MAX_WIDTH}px)`

/**
 * True on screens narrower than the mobile breakpoint (constants/app.ts), updated live when the window is resized
 * or a phone is rotated.
 *
 * `useSyncExternalStore` connects React to a value that lives outside it (here: the browser's media query). React
 * reads it during render, so the very first render already has the right layout, without a flash of the desktop
 * sidebar on a phone. (Ant Design's Grid.useBreakpoint only knows the size after the first render.)
 */
export function useIsMobile(): boolean {
  return useSyncExternalStore(subscribe, () => window.matchMedia(query).matches)
}

function subscribe(onChange: () => void): () => void {
  const media = window.matchMedia(query)
  media.addEventListener('change', onChange)
  return () => media.removeEventListener('change', onChange)
}
