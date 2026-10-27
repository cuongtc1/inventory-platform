import { App as AntApp, ConfigProvider } from 'antd'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './App'
import './styles/global.css'
import { theme } from './theme/theme'

/**
 * Entry point: puts the app into the page's <div id="root"> (index.html).
 *
 * - StrictMode: React's development checks (renders components twice in dev to reveal side effects).
 * - ConfigProvider: the Ant Design theme from theme.ts, for every component below it.
 * - AntApp: Ant Design's <App> wrapper, needed so message / notification / modal use the theme.
 * - From T2.1: the router goes here too.
 */
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ConfigProvider theme={theme}>
      <AntApp>
        <App />
      </AntApp>
    </ConfigProvider>
  </StrictMode>,
)
