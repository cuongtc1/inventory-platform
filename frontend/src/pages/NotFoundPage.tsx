import { Button, Result } from 'antd'
import { Link } from 'react-router'
import { ROUTES } from '../constants/routes'
import { texts } from '../messages/texts'

/** Any URL that matches no route (the "*" route in App.tsx). */
export function NotFoundPage() {
  return (
    <>
      <title>{texts.app.documentTitle(texts.pages.notFound.title)}</title>
      <Result
        status="404"
        title={texts.pages.notFound.title}
        subTitle={texts.pages.notFound.description}
        extra={
          <Link to={ROUTES.dashboard}>
            <Button type="primary">{texts.pages.notFound.backHome}</Button>
          </Link>
        }
      />
    </>
  )
}
