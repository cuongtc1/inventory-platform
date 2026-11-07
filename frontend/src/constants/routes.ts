/**
 * Every path in the app, in one place (docs/conventions.md section 10). Pages and links use these constants,
 * never a path string. The names follow the mockup's menu.
 */
export const ROUTES = {
  dashboard: '/',
  login: '/login',
  profile: '/profile',

  // Catalog (product-service)
  products: '/products',
  productNew: '/products/new',
  productEdit: (id: number | string) => `/products/${id}/edit`,
  categories: '/categories',
  categoryNew: '/categories/new',
  categoryEdit: (id: number | string) => `/categories/${id}/edit`,
  suppliers: '/suppliers',
  supplierNew: '/suppliers/new',
  supplierEdit: (id: number | string) => `/suppliers/${id}/edit`,

  // Inventory (Phase 4)
  warehouses: '/warehouses',
  warehouseNew: '/warehouses/new',
  warehouseEdit: (id: number | string) => `/warehouses/${id}/edit`,
  stockLevels: '/stock-levels',
  stockAdjust: '/stock-levels/adjust',
  stockAdjustFor: (productId: number, warehouseId: number) =>
    `/stock-levels/adjust?productId=${productId}&warehouseId=${warehouseId}`,
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
  userNew: '/users/new',
  userEdit: (id: number | string) => `/users/${id}/edit`,
} as const

/** The route patterns behind the path functions above, for the router: "/products/:id/edit". */
export const ROUTE_PATTERNS = {
  productEdit: '/products/:id/edit',
  categoryEdit: '/categories/:id/edit',
  supplierEdit: '/suppliers/:id/edit',
  warehouseEdit: '/warehouses/:id/edit',
  userEdit: '/users/:id/edit',
  salesOrderDetail: '/sales-orders/:id',
} as const
