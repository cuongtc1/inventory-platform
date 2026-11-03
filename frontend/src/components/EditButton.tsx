import { EditOutlined } from '@ant-design/icons'
import { Button, Tooltip } from 'antd'
import { useNavigate } from 'react-router'
import { useIsMobile } from '../hooks/useIsMobile'
import { useReturnState } from '../hooks/useReturnTo'
import { texts } from '../messages/texts'

interface EditButtonProps {
  /** The form's path, e.g. ROUTES.productEdit(7). */
  to: string
}

/**
 * The mockup's small "Edit" button in a table row. On phones only the icon (with a tooltip), to keep rows short.
 * Opens the form with the current list URL as `returnTo`, so Save and Cancel come back to the same list.
 */
export function EditButton({ to }: EditButtonProps) {
  const isMobile = useIsMobile()
  const navigate = useNavigate()
  const returnState = useReturnState()

  return (
    <Tooltip title={isMobile ? texts.common.actions.edit : undefined}>
      <Button
        size="small"
        icon={<EditOutlined />}
        aria-label={texts.common.actions.edit}
        onClick={() => navigate(to, { state: returnState })}
      >
        {!isMobile && texts.common.actions.edit}
      </Button>
    </Tooltip>
  )
}
