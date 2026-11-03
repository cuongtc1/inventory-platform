import { useEffect, useState } from 'react'
import type { SelectOption } from '../types/ui'
import { errorMessage } from '../utils/errors'

/** Loads the options once. A function declared outside the component, so it is the same on every render. */
export type OptionsLoader = (signal: AbortSignal) => Promise<SelectOption[]>

interface OptionsResult {
  options: SelectOption[]
  error?: string
}

/**
 * Options for a Select that come from the API (categories, suppliers, ...), loaded once when the component
 * appears, e.g. `useSelectOptions(getCategoryOptions)`.
 */
export function useSelectOptions(load: OptionsLoader) {
  const [result, setResult] = useState<OptionsResult>()

  useEffect(() => {
    const controller = new AbortController()
    load(controller.signal)
      .then(options => setResult({ options }))
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setResult({ options: [], error: errorMessage(error) })
        }
      })
    return () => controller.abort()
  }, [load])

  return {
    options: result?.options ?? [],
    loading: result === undefined,
    error: result?.error,
  }
}
