import { Button, Result } from 'antd'
import { type ReactNode, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { useReturnTo } from '../hooks/useReturnTo'
import { texts } from '../messages/texts'
import { errorMessage } from '../utils/errors'
import { PageHeader } from './PageHeader'
import { PageLoading } from './PageLoading'

interface RecordFormPageProps<T extends { id: number }> {
  /** Title on "/xxx/new". */
  newTitle: string
  /** Title on "/xxx/:id/edit", e.g. id => `Edit Product #${id}`. */
  editTitle: (id: string) => string
  /** Loads the record to edit, e.g. getProduct. Declared outside the component (same function on every render). */
  load: (id: number, signal: AbortSignal) => Promise<T>
  /** The list to go back to when the form wasn't opened from one (a bookmark, a typed URL). */
  listPath: string
  /**
   * The form: `record` is missing for a new one. `returnTo` is where Save and Cancel go: the list the form was
   * opened from, with its page and filters.
   */
  children: (record: T | undefined, returnTo: string) => ReactNode
}

/** What loading the record gave: the record, or the text of the error. */
type Loaded<T> = { id: number; record: T } | { id: number; error: string }

/**
 * The frame of every create / edit page (docs/conventions.md section 10, "Forms"): "/xxx/new" and "/xxx/:id/edit"
 * share it. It reads `:id` from the URL with `useParams()`, loads the record, and shows a spinner, the backend's
 * "not found" text, or "Page not found" for an id that isn't a number. The page only supplies the form.
 */
export function RecordFormPage<T extends { id: number }>({
  newTitle,
  editTitle,
  load,
  listPath,
  children,
}: RecordFormPageProps<T>) {
  const { id } = useParams()
  const recordId = id === undefined ? undefined : Number(id)
  const validId = recordId !== undefined && Number.isInteger(recordId) && recordId > 0
  const [loaded, setLoaded] = useState<Loaded<T>>()
  const returnTo = useReturnTo(listPath)
  const navigate = useNavigate()

  useEffect(() => {
    if (!validId) {
      return
    }
    const controller = new AbortController()
    load(recordId, controller.signal)
      .then(record => setLoaded({ id: recordId, record }))
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setLoaded({ id: recordId, error: errorMessage(error) })
        }
      })
    return () => controller.abort()
  }, [validId, recordId, load])

  const header = (
    <PageHeader
      title={id === undefined ? newTitle : editTitle(id)}
      extra={<Button onClick={() => navigate(returnTo)}>{texts.common.actions.back}</Button>}
    />
  )

  let content: ReactNode
  if (id === undefined) {
    content = children(undefined, returnTo)
  } else if (!validId) {
    // "/products/abc/edit": no request, just the 404 text
    content = <Result status="404" title={texts.pages.notFound.title} subTitle={texts.pages.notFound.description} />
  } else if (loaded?.id !== recordId) {
    // Still loading, or loaded for another id (the URL changed from one record to another)
    content = <PageLoading />
  } else if ('error' in loaded) {
    content = <Result status="warning" title={loaded.error} />
  } else {
    content = children(loaded.record, returnTo)
  }

  return (
    <>
      {header}
      {content}
    </>
  )
}
