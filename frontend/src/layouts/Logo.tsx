import { Link } from 'react-router'
import { ROUTES } from '../constants/routes'
import { texts } from '../messages/texts'
import { colors } from '../theme/theme'

interface LogoProps {
  /** Collapsed sidebar: only the "S" tile. */
  compact?: boolean
}

/** The mockup's logo: a green "S" tile and the app name, linking to the dashboard. */
export function Logo({ compact = false }: LogoProps) {
  return (
    <Link
      to={ROUTES.dashboard}
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: compact ? 'center' : 'flex-start',
        gap: 10,
        padding: compact ? '22px 0 18px' : '22px 22px 18px',
        color: colors.sidebarText,
        fontWeight: 700,
        fontSize: 18,
        textDecoration: 'none',
        whiteSpace: 'nowrap',
      }}
    >
      <span
        style={{
          display: 'grid',
          placeItems: 'center',
          flex: 'none',
          width: 30,
          height: 30,
          borderRadius: 9,
          background: colors.accent,
          color: '#04130a',
        }}
      >
        {texts.app.name.charAt(0)}
      </span>
      {!compact && texts.app.name}
    </Link>
  )
}
