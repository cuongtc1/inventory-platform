import { Typography } from 'antd'
import { texts } from '../messages/texts'
import type { Audited } from '../types/api'
import { formatDateTime } from '../utils/format'

/** The line under an edit form: "Created 22/10/2026 01:30 by system", "Last updated … by …" (Vietnam time). */
export function AuditInfo({ record }: { record: Audited }) {
  return (
    <Typography.Paragraph type="secondary" style={{ margin: '18px 0 0' }}>
      {texts.common.audit.created(formatDateTime(record.createdAt), record.createdBy)}
      <br />
      {texts.common.audit.updated(formatDateTime(record.updatedAt), record.updatedBy)}
    </Typography.Paragraph>
  )
}
