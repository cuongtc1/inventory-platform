import { Menu, type MenuProps } from 'antd'
import { createElement } from 'react'
import { Link, useLocation } from 'react-router'
import { activeNavPath, NAVIGATION } from '../constants/navigation'

type MenuItem = Required<MenuProps>['items'][number]

interface SidebarMenuProps {
  /** Icons only (collapsed desktop sidebar): section titles become dividers. */
  collapsed: boolean
  /** Called after an entry is clicked, e.g. to close the mobile menu. */
  onNavigate?: () => void
}

/**
 * The dark sidebar menu built from constants/navigation.ts. Each entry is a real link (<Link>), so it can also be
 * opened in a new tab; the entry for the current URL is highlighted.
 */
export function SidebarMenu({ collapsed, onNavigate }: SidebarMenuProps) {
  const location = useLocation()
  const selected = activeNavPath(location.pathname)

  const items: MenuItem[] = NAVIGATION.flatMap((section, index): MenuItem[] => {
    const entries: MenuItem[] = section.items.map(item => ({
      key: item.path,
      icon: createElement(item.icon),
      label: (
        <Link to={item.path} onClick={onNavigate}>
          {item.label}
        </Link>
      ),
    }))
    if (collapsed) {
      // No room for titles: a thin line between sections instead
      return index === 0 ? entries : [{ type: 'divider', key: `divider-${section.title}` }, ...entries]
    }
    return [{ type: 'group', key: section.title, label: section.title, children: entries }]
  })

  return (
    <Menu
      theme="dark"
      mode="inline"
      inlineCollapsed={collapsed}
      selectedKeys={selected ? [selected] : []}
      items={items}
      style={{ borderInlineEnd: 'none', paddingBottom: 24 }}
    />
  )
}
