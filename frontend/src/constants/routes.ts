/**
 * Every path in the app, in one place (docs/conventions.md section 10). Pages and links use these constants,
 * never a path string. The names follow the mockup's menu.
 */
export const ROUTES = {
  dashboard: '/',
  login: '/login',

  // Catalog (product-service)
  products: '/products',
  productNew: '/products/new',
  productEdit: (id: number | string) => `/products/${id}/edit`,
  categories: '/categories',
  suppliers: '/suppliers',

  // Inventory (Phase 4)
  warehouses: '/warehouses',
  stockLevels: '/stock-levels',
  stockMovements: '/stock-movements',
  lowStock: '/low-stock',

  // Orders (Phases 5-7)
  purchaseOrders: '/purchase-orders',
  salesOrders: '/sales-orders',
  salesOrderNew: '/sales-orders/new',
  salesOrderDetail: (id: number | string) => `/sales-orders/${id}`,
  customers: '/customers',

  // Insights and admin
  reports: '/reports',
  notifications: '/notifications',
  users: '/users',
} as const

/** The route pattern behind productEdit(), for the router: "/products/:id/edit". */
export const ROUTE_PATTERNS = {
  productEdit: '/products/:id/edit',
  salesOrderDetail: '/sales-orders/:id',
} as const
