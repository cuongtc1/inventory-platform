import {
  AppstoreOutlined,
  BarChartOutlined,
  BellOutlined,
  DashboardOutlined,
  DownloadOutlined,
  HomeOutlined,
  ShoppingCartOutlined,
  SwapOutlined,
  TagOutlined,
  TeamOutlined,
  TruckOutlined,
  UnorderedListOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons'
import type { ComponentType } from 'react'
import { texts } from '../messages/texts'
import { ROUTES } from './routes'

/** One entry of the sidebar: where it goes, what it says, which icon it shows. */
export interface NavItem {
  path: string
  label: string
  icon: ComponentType
}

/** A titled group of entries ("Catalog", "Inventory", ...). */
export interface NavSection {
  title: string
  items: NavItem[]
}

/**
 * The sidebar menu, in the mockup's order (docs/mockup/app.js, NAV). The layout builds the Ant Design Menu from
 * this list, so adding a page to the menu is one line here. Role-based hiding ("View as") comes with login, T3.5.
 */
export const NAVIGATION: NavSection[] = [
  {
    title: texts.nav.sections.overview,
    items: [{ path: ROUTES.dashboard, label: texts.nav.items.dashboard, icon: DashboardOutlined }],
  },
  {
    title: texts.nav.sections.catalog,
    items: [
      { path: ROUTES.products, label: texts.nav.items.products, icon: AppstoreOutlined },
      { path: ROUTES.categories, label: texts.nav.items.categories, icon: TagOutlined },
      { path: ROUTES.suppliers, label: texts.nav.items.suppliers, icon: TruckOutlined },
    ],
  },
  {
    title: texts.nav.sections.inventory,
    items: [
      { path: ROUTES.warehouses, label: texts.nav.items.warehouses, icon: HomeOutlined },
      { path: ROUTES.stockLevels, label: texts.nav.items.stockLevels, icon: UnorderedListOutlined },
      { path: ROUTES.stockMovements, label: texts.nav.items.stockMovements, icon: SwapOutlined },
      { path: ROUTES.lowStock, label: texts.nav.items.lowStock, icon: WarningOutlined },
    ],
  },
  {
    title: texts.nav.sections.orders,
    items: [
      { path: ROUTES.purchaseOrders, label: texts.nav.items.purchaseOrders, icon: DownloadOutlined },
      { path: ROUTES.salesOrders, label: texts.nav.items.salesOrders, icon: ShoppingCartOutlined },
      { path: ROUTES.customers, label: texts.nav.items.customers, icon: TeamOutlined },
    ],
  },
  {
    title: texts.nav.sections.insights,
    items: [
      { path: ROUTES.reports, label: texts.nav.items.reports, icon: BarChartOutlined },
      { path: ROUTES.notifications, label: texts.nav.items.notifications, icon: BellOutlined },
    ],
  },
  {
    title: texts.nav.sections.admin,
    items: [{ path: ROUTES.users, label: texts.nav.items.users, icon: UserOutlined }],
  },
]

/**
 * The menu entry to highlight for a URL: the one whose path is the longest match, so "/products/7/edit" lights
 * up "Products". The dashboard ("/") only matches itself, otherwise it would match every URL.
 */
export function activeNavPath(pathname: string): string | undefined {
  const paths = NAVIGATION.flatMap(section => section.items.map(item => item.path))
  return paths
    .filter(path => (path === ROUTES.dashboard ? pathname === path : pathname === path || pathname.startsWith(`${path}/`)))
    .sort((a, b) => b.length - a.length)[0]
}
