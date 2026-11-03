import type { ThemeConfig } from 'antd'
import { SIDEBAR_COLLAPSED_WIDTH } from '../constants/app'

/**
 * The mockup's look (docs/mockup/style.css) as Ant Design theme settings. Components take their colours from
 * here; pages never hard-code a colour.
 *
 * Ant Design calls these settings "design tokens": colorPrimary, borderRadius, ... One change here restyles
 * every button, table and input.
 */
export const colors = {
  primary: '#16a34a',
  background: '#f2f6f3',
  card: '#ffffff',
  border: '#e0e8e3',
  text: '#16221b',
  textMuted: '#69796f',
  red: '#dc2626',
  amber: '#b45309',
  blue: '#0369a1',
  sidebar: '#09100c',
  sidebarBorder: '#1b2820',
  sidebarText: '#e3eee7',
  sidebarSection: '#6f8a7b',
  accent: '#2fe07a',
  // Sidebar entries: normal text, hover and selected backgrounds (mockup `aside a.nv`, `a.nv:hover`, `a.on`)
  sidebarItem: '#b5c9bd',
  sidebarItemHover: '#132119',
  sidebarItemSelected: '#123d25',
  // Avatar circle in the top bar (mockup `.av`)
  avatarBackground: '#d6f3e1',
  avatarText: '#0f7a37',
} as const

/** Badge tones from the mockup (`.bd` + `.r` / `.y` / `.b`): light background, darker text. */
export const badgeTones = {
  green: { background: '#e3f6ea', color: '#0f7a37' },
  red: { background: '#fde8e8', color: colors.red },
  amber: { background: '#fef3c7', color: colors.amber },
  blue: { background: '#e0f2fe', color: colors.blue },
  // Not in the mockup: for an enum value the frontend doesn't know yet
  grey: { background: '#edf1ee', color: colors.textMuted },
} as const

export type BadgeTone = keyof typeof badgeTones

export const theme: ThemeConfig = {
  token: {
    colorPrimary: colors.primary,
    colorSuccess: colors.primary,
    colorError: colors.red,
    colorWarning: colors.amber,
    colorInfo: colors.blue,
    // Links are green in the mockup; without this Ant Design takes the link colour from colorInfo (blue)
    colorLink: colors.primary,
    colorText: colors.text,
    colorTextSecondary: colors.textMuted,
    colorBorder: colors.border,
    colorBorderSecondary: colors.border,
    colorBgLayout: colors.background,
    colorBgContainer: colors.card,
    borderRadius: 9,
    fontFamily: '"Segoe UI", system-ui, sans-serif',
    fontSize: 14,
  },
  components: {
    Layout: {
      siderBg: colors.sidebar,
      headerBg: colors.card,
      bodyBg: colors.background,
    },
    Menu: {
      darkItemBg: colors.sidebar,
      darkSubMenuItemBg: colors.sidebar,
      darkItemColor: colors.sidebarItem,
      darkItemHoverColor: colors.sidebarText,
      darkItemHoverBg: colors.sidebarItemHover,
      darkItemSelectedBg: colors.sidebarItemSelected,
      darkItemSelectedColor: colors.accent,
      darkGroupTitleColor: colors.sidebarSection,
      // Lines between sections in the collapsed sidebar
      colorSplit: colors.sidebarBorder,
      // Same as the collapsed sidebar (Ant Design's default is 80, which would overflow it)
      collapsedWidth: SIDEBAR_COLLAPSED_WIDTH,
      itemBorderRadius: 8,
      itemMarginInline: 12,
      groupTitleFontSize: 12,
    },
    // The mockup's `.card`: white, rounded 14 px, 20 px inside
    Card: {
      borderRadiusLG: 14,
      bodyPadding: 20,
    },
    Table: {
      headerBg: '#f6faf7',
      headerColor: colors.textMuted,
      headerSplitColor: 'transparent',
      rowHoverBg: '#f6faf7',
      // The mockup doesn't shade the sorted column; the arrow in its header is enough
      bodySortBg: colors.card,
      headerSortActiveBg: '#f6faf7',
      headerSortHoverBg: '#eef4f0',
    },
  },
}
