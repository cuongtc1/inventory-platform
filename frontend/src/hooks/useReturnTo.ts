import { useLocation } from 'react-router'

/**
 * Going from a list to a form and back to the same list (same page, filters and sort).
 *
 * Links from a list to a form pass `state={useReturnState()}`: React Router keeps this `state` with the history
 * entry, invisible in the URL. The form then goes back to `useReturnTo(ROUTES.products)`: the list URL it was
 * opened from, or the plain list when the form was opened directly (a bookmark, a typed URL).
 */
interface ReturnState {
  returnTo: string
}

/** The `state` for a link from the current page to a form. */
export function useReturnState(): ReturnState {
  const location = useLocation()
  return { returnTo: location.pathname + location.search }
}

/** Where a form goes after Save or Cancel. */
export function useReturnTo(fallback: string): string {
  const state: unknown = useLocation().state
  return isReturnState(state) ? state.returnTo : fallback
}

function isReturnState(state: unknown): state is ReturnState {
  return (
    typeof state === 'object' &&
    state !== null &&
    'returnTo' in state &&
    typeof state.returnTo === 'string' &&
    // Only paths inside this app
    state.returnTo.startsWith('/')
  )
}
