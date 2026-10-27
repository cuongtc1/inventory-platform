import { Flex, Spin } from 'antd'
import { texts } from '../messages/texts'

/** A centred spinner while a page (or its code) is loading. */
export function PageLoading() {
  return (
    <Flex justify="center" align="center" style={{ minHeight: 240 }}>
      <Spin description={texts.common.feedback.loading} />
    </Flex>
  )
}
