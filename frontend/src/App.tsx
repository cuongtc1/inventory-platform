import { createBrowserRouter, type RouteObject } from 'react-router'
import { RouterProvider } from 'react-router/dom'
import { PageLoading } from './components/PageLoading'
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
 * Later-phase pages are routes already, showing ComingSoonPage, so the menu (T2.2) can link to all of them.
 */
const comingSoon = (title: string, phase: number) => <ComingSoonPage title={title} phase={texts.pages.phase(phase)} />

const routes: RouteObject[] = [
  {
    path: ROUTES.dashboard,
    Component: AppLayout,
    // Shown while the first page's code loads, and when a page fails to render
    HydrateFallback: PageLoading,
    ErrorBoundary: RouteErrorPage,
    children: [
      {
        index: true,
        lazy: { Component: () => import('./pages/dashboard/DashboardPage').then(module => module.DashboardPage) },
      },

      // Catalog (Phase 2)
      {
        path: ROUTES.products,
        lazy: { Component: () => import('./pages/products/ProductListPage').then(module => module.ProductListPage) },
      },
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
        path: ROUTES.suppliers,
        lazy: { Component: () => import('./pages/suppliers/SupplierListPage').then(module => module.SupplierListPage) },
      },

      // Later phases
      { path: ROUTES.profile, element: comingSoon(texts.layout.profile, 3) },
      { path: ROUTES.users, element: comingSoon(texts.nav.items.users, 3) },
      { path: ROUTES.warehouses, element: comingSoon(texts.nav.items.warehouses, 4) },
      { path: ROUTES.stockLevels, element: comingSoon(texts.nav.items.stockLevels, 4) },
      { path: ROUTES.stockMovements, element: comingSoon(texts.nav.items.stockMovements, 4) },
      { path: ROUTES.lowStock, element: comingSoon(texts.nav.items.lowStock, 4) },
      { path: ROUTES.customers, element: comingSoon(texts.nav.items.customers, 5) },
      { path: ROUTES.salesOrders, element: comingSoon(texts.nav.items.salesOrders, 5) },
      { path: ROUTES.salesOrderNew, element: comingSoon(texts.nav.items.salesOrders, 5) },
      { path: ROUTE_PATTERNS.salesOrderDetail, element: comingSoon(texts.nav.items.salesOrders, 5) },
      { path: ROUTES.purchaseOrders, element: comingSoon(texts.nav.items.purchaseOrders, 7) },
      { path: ROUTES.notifications, element: comingSoon(texts.nav.items.notifications, 8) },
      { path: ROUTES.reports, element: comingSoon(texts.nav.items.reports, 9) },

      // Any other URL
      { path: '*', Component: NotFoundPage },
    ],
  },
  // Login has no sidebar or top bar, so it sits outside AppLayout (Phase 3)
  {
    path: ROUTES.login,
    element: <div style={{ padding: 24 }}>{comingSoon(texts.app.name, 3)}</div>,
    ErrorBoundary: RouteErrorPage,
  },
]

const router = createBrowserRouter(routes)

/** The whole app: the router decides which layout and page to show for the current URL. */
export function App() {
  return <RouterProvider router={router} />
}
