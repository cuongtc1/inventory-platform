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
import type { UserType } from '../types/auth'
import { ROUTES } from './routes'
import { hasRole } from './userType'

/** One entry of the sidebar: where it goes, what it says, which icon it shows. */
export interface NavItem {
  path: string
  label: string
  icon: ComponentType
  /** The lowest role that sees this entry; missing = everyone. */
  role?: UserType
}

/** A titled group of entries ("Catalog", "Inventory", ...). */
export interface NavSection {
  title: string
  items: NavItem[]
}

/**
 * The sidebar menu, in the mockup's order (docs/mockup/app.js, NAV). The layout builds the Ant Design Menu from
 * this list, so adding a page to the menu is one line here. `role` is the mockup's level (1 = none, 2 = MANAGER, 3 = ADMIN).
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
      { path: ROUTES.categories, label: texts.nav.items.categories, role: 'MANAGER', icon: TagOutlined },
      { path: ROUTES.suppliers, label: texts.nav.items.suppliers, role: 'MANAGER', icon: TruckOutlined },
    ],
  },
  {
    title: texts.nav.sections.inventory,
    items: [
      { path: ROUTES.warehouses, label: texts.nav.items.warehouses, role: 'MANAGER', icon: HomeOutlined },
      { path: ROUTES.stockLevels, label: texts.nav.items.stockLevels, icon: UnorderedListOutlined },
      { path: ROUTES.stockMovements, label: texts.nav.items.stockMovements, icon: SwapOutlined },
      { path: ROUTES.lowStock, label: texts.nav.items.lowStock, icon: WarningOutlined },
    ],
  },
  {
    title: texts.nav.sections.orders,
    items: [
      { path: ROUTES.purchaseOrders, label: texts.nav.items.purchaseOrders, role: 'MANAGER', icon: DownloadOutlined },
      { path: ROUTES.salesOrders, label: texts.nav.items.salesOrders, icon: ShoppingCartOutlined },
      { path: ROUTES.customers, label: texts.nav.items.customers, icon: TeamOutlined },
    ],
  },
  {
    title: texts.nav.sections.insights,
    items: [
      { path: ROUTES.reports, label: texts.nav.items.reports, role: 'MANAGER', icon: BarChartOutlined },
      { path: ROUTES.notifications, label: texts.nav.items.notifications, icon: BellOutlined },
    ],
  },
  {
    title: texts.nav.sections.admin,
    items: [{ path: ROUTES.users, label: texts.nav.items.users, role: 'ADMIN', icon: UserOutlined }],
  },
]

/** The menu for a role: entries of higher roles are left out, and so are sections that end up empty. */
export function navigationFor(userType: UserType): NavSection[] {
  return NAVIGATION.map(section => ({
    ...section,
    items: section.items.filter(item => !item.role || hasRole(userType, item.role)),
  })).filter(section => section.items.length > 0)
}

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
