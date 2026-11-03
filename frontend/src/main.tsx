import { App as AntApp, ConfigProvider } from 'antd'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import { texts } from './messages/texts'
import './styles/global.css'
import { theme } from './theme/theme'

/**
 * Entry point: puts the app into the page's <div id="root"> (index.html).
 *
 * - StrictMode: React's development checks (renders components twice in dev to reveal side effects).
 * - ConfigProvider: the Ant Design theme from theme.ts, for every component below it, and the texts of the
 *   forms' own checks (e.g. "SKU is required") from messages/texts.ts.
 * - AntApp: Ant Design's <App> wrapper, needed so message / notification / modal use the theme.
 * - App: the router (App.tsx), which picks the layout and page for the current URL.
 */
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ConfigProvider theme={theme} form={{ validateMessages: { required: texts.common.validation.required } }}>
      <AntApp>
        <App />
      </AntApp>
    </ConfigProvider>
  </StrictMode>,
)
