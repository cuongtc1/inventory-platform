import { Typography } from 'antd'

interface ProductCellProps {
  name: string
  sku: string
}

/** A product in an inventory table: the mockup's name, with the SKU beside it because search also finds SKUs. */
export function ProductCell({ name, sku }: ProductCellProps) {
  return (
    <>
      {name} <Typography.Text type="secondary">{sku}</Typography.Text>
    </>
  )
}
