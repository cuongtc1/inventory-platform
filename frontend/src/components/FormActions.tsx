import { Button, Flex } from 'antd'
import { texts } from '../messages/texts'

interface FormActionsProps {
  /** True while the save request runs: the button shows a spinner. */
  saving: boolean
  onCancel: () => void
}

/** The buttons under every form, like the mockup: "Save changes" (submits the form) and "Cancel". */
export function FormActions({ saving, onCancel }: FormActionsProps) {
  return (
    <Flex gap={8} wrap>
      <Button type="primary" htmlType="submit" loading={saving}>
        {texts.common.actions.saveChanges}
      </Button>
      <Button onClick={onCancel}>{texts.common.actions.cancel}</Button>
    </Flex>
  )
}
