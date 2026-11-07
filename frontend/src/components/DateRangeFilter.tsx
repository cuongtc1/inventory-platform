import { DatePicker, Flex } from 'antd'
import { useIsMobile } from '../hooks/useIsMobile'
import type { PagedList } from '../hooks/usePagedList'
import { texts } from '../messages/texts'
import { DISPLAY_DATE_FORMAT, parseApiDate, toApiDate } from '../utils/format'

interface DateRangeFilterProps<T> {
  list: PagedList<T>
}

/**
 * A from / to date filter, kept in the URL as `?from=2026-10-01&to=2026-10-31`. Either end may stay empty.
 *
 * Wide screens get one RangePicker. It shows two months side by side, which doesn't fit a phone, so phones get two
 * single pickers with one month each.
 *
 * Ant Design's date pickers work with dayjs objects. The rest of the app doesn't touch dayjs: it is converted here
 * and in utils/format.ts (docs/conventions.md section 10).
 */
export function DateRangeFilter<T>({ list }: DateRangeFilterProps<T>) {
  const isMobile = useIsMobile()
  const from = parseApiDate(list.filter('from'))
  const to = parseApiDate(list.filter('to'))

  if (isMobile) {
    return (
      <Flex gap={10} style={{ width: '100%' }}>
        <DatePicker
          value={from}
          maxDate={to}
          format={DISPLAY_DATE_FORMAT}
          placeholder={texts.common.filters.fromDate}
          onChange={date => list.setFilters({ from: date ? toApiDate(date) : undefined })}
          // The calendar is enough on a phone; no keyboard covering half of it
          inputReadOnly
          style={{ flex: 1, minWidth: 0 }}
        />
        <DatePicker
          value={to}
          minDate={from}
          format={DISPLAY_DATE_FORMAT}
          placeholder={texts.common.filters.toDate}
          onChange={date => list.setFilters({ to: date ? toApiDate(date) : undefined })}
          inputReadOnly
          style={{ flex: 1, minWidth: 0 }}
        />
      </Flex>
    )
  }

  return (
    <DatePicker.RangePicker
      value={[from, to]}
      format={DISPLAY_DATE_FORMAT}
      allowEmpty={[true, true]}
      placeholder={[texts.common.filters.fromDate, texts.common.filters.toDate]}
      onChange={dates =>
        list.setFilters({
          from: dates?.[0] ? toApiDate(dates[0]) : undefined,
          to: dates?.[1] ? toApiDate(dates[1]) : undefined,
        })
      }
      style={{ width: 260 }}
    />
  )
}
