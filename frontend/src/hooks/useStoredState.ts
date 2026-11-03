import { useState } from 'react'

/**
 * Like `useState`, but the value survives a page reload: it is kept in the browser's localStorage under `key`.
 * Only for small per-browser preferences (e.g. a collapsed sidebar), never for data that matters: localStorage
 * can be empty or blocked (private mode, cleared site data), and then the default is used.
 *
 * A custom hook is a function whose name starts with "use" and that calls other hooks; it lets several
 * components share stateful logic.
 */
export function useStoredState<T>(key: string, defaultValue: T): [T, (value: T) => void] {
  const [value, setValue] = useState<T>(() => read(key, defaultValue))

  const update = (next: T) => {
    setValue(next)
    try {
      window.localStorage.setItem(key, JSON.stringify(next))
    } catch {
      // Storage blocked or full: keep the value for this visit only
    }
  }

  return [value, update]
}

function read<T>(key: string, defaultValue: T): T {
  try {
    const stored = window.localStorage.getItem(key)
    return stored === null ? defaultValue : (JSON.parse(stored) as T)
  } catch {
    return defaultValue
  }
}
