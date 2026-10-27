import { Button, Result } from 'antd'
import { useRouteError } from 'react-router'
import { texts } from '../messages/texts'

/**
 * Shown instead of a page when rendering it throws (a bug) or its code can't be loaded. React Router catches the
 * error at the route's ErrorBoundary, so the rest of the app keeps working. Details go to the browser console only.
 */
export function RouteErrorPage() {
  const error = useRouteError()
  console.error(error)

  return (
    <>
      <title>{texts.app.documentTitle(texts.pages.error.title)}</title>
      <Result
        status="error"
        title={texts.pages.error.title}
        subTitle={isChunkLoadError(error) ? texts.pages.error.newVersion : texts.pages.error.description}
        extra={
          <Button type="primary" onClick={() => window.location.reload()}>
            {texts.pages.error.reload}
          </Button>
        }
      />
    </>
  )
}

/**
 * A lazy page's file is missing: the browser still has the old index page after a new version was deployed.
 * Reloading fetches the new file names.
 */
function isChunkLoadError(error: unknown): boolean {
  return error instanceof TypeError && /dynamically imported module|Importing a module script failed/i.test(error.message)
}
