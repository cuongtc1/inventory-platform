import { DeleteOutlined } from '@ant-design/icons'
import { Button, Popconfirm, Tooltip } from 'antd'
import { useIsMobile } from '../hooks/useIsMobile'
import { texts } from '../messages/texts'

interface DeleteButtonProps {
  /** The question, e.g. `Delete "Wireless Mouse"?`. Defaults to a generic one. */
  title?: string
  /**
   * Deletes the record. While the returned Promise runs, the confirm button shows a spinner; the page handles
   * success (message, reload) and errors itself.
   */
  onConfirm: () => Promise<void>
}

/**
 * The mockup's small red "Delete" button in a table row, with a confirmation first (deletes are permanent,
 * docs/conventions.md section 5). On phones only the icon.
 */
export function DeleteButton({ title, onConfirm }: DeleteButtonProps) {
  const isMobile = useIsMobile()
  const confirm = texts.common.confirmDelete

  return (
    <Popconfirm
      title={title ?? confirm.title}
      description={confirm.content}
      okText={confirm.ok}
      cancelText={confirm.cancel}
      okButtonProps={{ danger: true }}
      onConfirm={onConfirm}
    >
      <Tooltip title={isMobile ? texts.common.actions.delete : undefined}>
        <Button size="small" danger icon={<DeleteOutlined />} aria-label={texts.common.actions.delete}>
          {!isMobile && texts.common.actions.delete}
        </Button>
      </Tooltip>
    </Popconfirm>
  )
}
