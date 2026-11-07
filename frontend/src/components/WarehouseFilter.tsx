import { Select } from 'antd'
import { getWarehouseOptions } from '../api/warehouses'
import { useFilterWidth } from '../hooks/useIsMobile'
import type { PagedList } from '../hooks/usePagedList'
import { useSelectOptions } from '../hooks/useSelectOptions'
import { texts } from '../messages/texts'
import { toId } from '../utils/table'

interface WarehouseFilterProps<T> {
  list: PagedList<T>
}

/** The "All warehouses" Select of the inventory lists. Keeps its value in the URL as `?warehouseId=...`. */
export function WarehouseFilter<T>({ list }: WarehouseFilterProps<T>) {
  const filterWidth = useFilterWidth()
  const warehouses = useSelectOptions(getWarehouseOptions)

  return (
    <Select
      value={toId(list.filter('warehouseId'))}
      options={warehouses.options}
      loading={warehouses.loading}
      placeholder={texts.common.filters.allWarehouses}
      allowClear
      showSearch={{ optionFilterProp: 'label' }}
      onChange={(warehouseId?: number) => list.setFilters({ warehouseId })}
      style={filterWidth(220)}
    />
  )
}
