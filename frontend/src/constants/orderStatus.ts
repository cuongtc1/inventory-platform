import { texts } from '../messages/texts'
import type { OrderAction, OrderStatus } from '../types/salesOrder'
import type { EnumDisplay } from '../types/ui'

/** Labels and badge colours for OrderStatus, with the mockup's tones (BD in docs/mockup/app.js). */
export const ORDER_STATUS: Record<OrderStatus, EnumDisplay> = {
  PENDING: { label: texts.salesOrder.status.PENDING, tone: 'amber' },
  RESERVED: { label: texts.salesOrder.status.RESERVED, tone: 'blue' },
  PAID: { label: texts.salesOrder.status.PAID, tone: 'green' },
  SHIPPED: { label: texts.salesOrder.status.SHIPPED, tone: 'blue' },
  COMPLETED: { label: texts.salesOrder.status.COMPLETED, tone: 'green' },
  REJECTED: { label: texts.salesOrder.status.REJECTED, tone: 'red' },
  CANCELLED: { label: texts.salesOrder.status.CANCELLED, tone: 'red' },
}

/** Options for a status filter, in the order an order goes through them. */
export const ORDER_STATUS_OPTIONS = (Object.keys(ORDER_STATUS) as OrderStatus[]).map(status => ({
  value: status,
  label: ORDER_STATUS[status].label,
}))

/**
 * A copy of the backend's state machine (`OrderStatus.nextStatuses()` in order-service), only to decide which buttons
 * to show. The backend checks every change again and answers 409 if this copy is ever out of date.
 */
export const NEXT_STATUSES: Record<OrderStatus, OrderStatus[]> = {
  PENDING: ['RESERVED', 'REJECTED', 'CANCELLED'],
  RESERVED: ['PAID', 'CANCELLED'],
  PAID: ['SHIPPED', 'CANCELLED'],
  SHIPPED: ['COMPLETED'],
  COMPLETED: [],
  REJECTED: [],
  CANCELLED: [],
}

/** The buttons of the detail page: which action moves the order to which status. RESERVED and REJECTED come from the saga. */
export const ORDER_ACTIONS: { action: OrderAction; to: OrderStatus; label: string; done: (orderNumber: string) => string }[] = [
  { action: 'pay', to: 'PAID', label: texts.salesOrder.actions.pay, done: texts.salesOrder.actions.paid },
  { action: 'ship', to: 'SHIPPED', label: texts.salesOrder.actions.ship, done: texts.salesOrder.actions.shipped },
  { action: 'complete', to: 'COMPLETED', label: texts.salesOrder.actions.complete, done: texts.salesOrder.actions.completed },
]

/** The stepper of the detail page, like the mockup: one step per status of the happy path. */
export const ORDER_STEPS: { status: OrderStatus; title: string }[] = [
  { status: 'PENDING', title: texts.salesOrder.steps.created },
  { status: 'RESERVED', title: texts.salesOrder.steps.reserved },
  { status: 'PAID', title: texts.salesOrder.steps.paid },
  { status: 'SHIPPED', title: texts.salesOrder.steps.shipped },
  { status: 'COMPLETED', title: texts.salesOrder.steps.done },
]
