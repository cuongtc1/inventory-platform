import { createBrowserRouter, type RouteObject } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { PageLoading } from './components/PageLoading'
import { RequireLogin } from './components/RequireLogin'
import { RequireRole } from './components/RequireRole'
import { ROUTE_PATTERNS, ROUTES } from './constants/routes'
import { AppLayout } from './layouts/AppLayout'
import { texts } from './messages/texts'
import { ComingSoonPage } from './pages/ComingSoonPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { RouteErrorPage } from './pages/RouteErrorPage'

/**
 * The route table: which page shows for which URL (paths from constants/routes.ts).
 *
 * Lazy routes: `lazy: { Component: ... }` downloads a page's code only when it is first visited, so the first
 * download stays small and each page becomes its own file in the build. `import()` (with parentheses) is the
 * JavaScript "dynamic import"; Vite turns each one into a separate chunk.
 *
 * Later-phase pages are routes already, showing ComingSoonPage, so the menu can link to all of them.
 *
 * Route guards: everything except login sits under RequireLogin, and pages of higher roles under RequireRole. A
 * route with only an `element` or `Component` and no `path` adds no URL: it wraps its children.
 */
const comingSoon = (title: string, phase: number) => <ComingSoonPage title={title} phase={texts.pages.phase(phase)} />

const routes: RouteObject[] = [
  {
    path: ROUTES.dashboard,
    Component: RequireLogin,
    // Shown while the first page's code loads, and when a page fails to render
    HydrateFallback: PageLoading,
    ErrorBoundary: RouteErrorPage,
    children: [
      {
        Component: AppLayout,
        children: [
          {
            index: true,
            lazy: { Component: () => import('./pages/dashboard/DashboardPage').then(module => module.DashboardPage) },
          },
          {
            path: ROUTES.profile,
            lazy: { Component: () => import('./pages/profile/ProfilePage').then(module => module.ProfilePage) },
          },

          // Every signed-in user
          {
            path: ROUTES.products,
            lazy: { Component: () => import('./pages/products/ProductListPage').then(module => module.ProductListPage) },
          },
          {
            path: ROUTES.stockLevels,
            lazy: { Component: () => import('./pages/stock/StockLevelListPage').then(module => module.StockLevelListPage) },
          },
          {
            path: ROUTES.stockMovements,
            lazy: { Component: () => import('./pages/stock/StockMovementListPage').then(module => module.StockMovementListPage) },
          },
          {
            path: ROUTES.lowStock,
            lazy: { Component: () => import('./pages/stock/LowStockListPage').then(module => module.LowStockListPage) },
          },
          // Staff take orders, so they add and edit customers too; deleting is hidden for them on the list
          {
            path: ROUTES.customers,
            lazy: { Component: () => import('./pages/customers/CustomerListPage').then(module => module.CustomerListPage) },
          },
          {
            path: ROUTES.customerNew,
            lazy: { Component: () => import('./pages/customers/CustomerFormPage').then(module => module.CustomerFormPage) },
          },
          {
            path: ROUTE_PATTERNS.customerEdit,
            lazy: { Component: () => import('./pages/customers/CustomerFormPage').then(module => module.CustomerFormPage) },
          },
          {
            path: ROUTES.salesOrders,
            lazy: { Component: () => import('./pages/salesOrders/SalesOrderListPage').then(module => module.SalesOrderListPage) },
          },
          {
            path: ROUTES.salesOrderNew,
            lazy: { Component: () => import('./pages/salesOrders/SalesOrderNewPage').then(module => module.SalesOrderNewPage) },
          },
          {
            path: ROUTE_PATTERNS.salesOrderDetail,
            lazy: { Component: () => import('./pages/salesOrders/SalesOrderDetailPage').then(module => module.SalesOrderDetailPage) },
          },
          { path: ROUTES.notifications, element: comingSoon(texts.nav.items.notifications, 8) },

          // MANAGER and up, like the role of the same entries in constants/navigation.ts
          {
            element: <RequireRole role="MANAGER" />,
            children: [
              {
                path: ROUTES.productNew,
                lazy: { Component: () => import('./pages/products/ProductFormPage').then(module => module.ProductFormPage) },
              },
              {
                path: ROUTE_PATTERNS.productEdit,
                lazy: { Component: () => import('./pages/products/ProductFormPage').then(module => module.ProductFormPage) },
              },
              {
                path: ROUTES.categories,
                lazy: { Component: () => import('./pages/categories/CategoryListPage').then(module => module.CategoryListPage) },
              },
              {
                path: ROUTES.categoryNew,
                lazy: { Component: () => import('./pages/categories/CategoryFormPage').then(module => module.CategoryFormPage) },
              },
              {
                path: ROUTE_PATTERNS.categoryEdit,
                lazy: { Component: () => import('./pages/categories/CategoryFormPage').then(module => module.CategoryFormPage) },
              },
              {
                path: ROUTES.suppliers,
                lazy: { Component: () => import('./pages/suppliers/SupplierListPage').then(module => module.SupplierListPage) },
              },
              {
                path: ROUTES.supplierNew,
                lazy: { Component: () => import('./pages/suppliers/SupplierFormPage').then(module => module.SupplierFormPage) },
              },
              {
                path: ROUTE_PATTERNS.supplierEdit,
                lazy: { Component: () => import('./pages/suppliers/SupplierFormPage').then(module => module.SupplierFormPage) },
              },
              {
                path: ROUTES.warehouses,
                lazy: { Component: () => import('./pages/warehouses/WarehouseListPage').then(module => module.WarehouseListPage) },
              },
              {
                path: ROUTES.warehouseNew,
                lazy: { Component: () => import('./pages/warehouses/WarehouseFormPage').then(module => module.WarehouseFormPage) },
              },
              {
                path: ROUTE_PATTERNS.warehouseEdit,
                lazy: { Component: () => import('./pages/warehouses/WarehouseFormPage').then(module => module.WarehouseFormPage) },
              },
              {
                path: ROUTES.stockAdjust,
                lazy: { Component: () => import('./pages/stock/StockAdjustPage').then(module => module.StockAdjustPage) },
              },
              { path: ROUTES.purchaseOrders, element: comingSoon(texts.nav.items.purchaseOrders, 7) },
              { path: ROUTES.reports, element: comingSoon(texts.nav.items.reports, 9) },
            ],
          },

          // ADMIN
          {
            element: <RequireRole role="ADMIN" />,
            children: [
              {
                path: ROUTES.users,
                lazy: { Component: () => import('./pages/users/UserListPage').then(module => module.UserListPage) },
              },
              {
                path: ROUTES.userNew,
                lazy: { Component: () => import('./pages/users/UserFormPage').then(module => module.UserFormPage) },
              },
              {
                path: ROUTE_PATTERNS.userEdit,
                lazy: { Component: () => import('./pages/users/UserFormPage').then(module => module.UserFormPage) },
              },
            ],
          },

          // Any other URL
          { path: '*', Component: NotFoundPage },
        ],
      },
    ],
  },
  // Login has no sidebar or top bar, so it sits outside AppLayout and RequireLogin
  {
    path: ROUTES.login,
    lazy: { Component: () => import('./pages/login/LoginPage').then(module => module.LoginPage) },
    HydrateFallback: PageLoading,
    ErrorBoundary: RouteErrorPage,
  },
]

const router = createBrowserRouter(routes)

/** The whole app: the router decides which layout and page to show for the current URL. */
export function App() {
  return <RouterProvider router={router} />
}
