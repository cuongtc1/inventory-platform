import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

/**
 * Vite configuration (https://vite.dev/config/).
 *
 * Dev proxy (docs/conventions.md section 10): the browser calls "/api/..." on localhost:5173, and the Vite dev
 * server forwards it to the backend. So the browser only ever sees one origin, and the services need no CORS
 * settings. The target is product-service in Phase 2, the api-gateway (http://localhost:8080) from Phase 3.
 * Change it with API_PROXY_TARGET in frontend/.env (see .env.example); it has no VITE_ prefix on purpose, so it
 * stays in the dev server and never reaches the browser.
 */
export default defineConfig(({ mode }) => {
  // loadEnv reads frontend/.env* for this mode; '' = all variables, not only VITE_*
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.API_PROXY_TARGET || 'http://localhost:8082'

  return {
    plugins: [react()],
    server: {
      port: 5173,
      // Fail instead of silently moving to 5174: the port is part of the documented setup
      strictPort: true,
      proxy: {
        '/api': { target: apiTarget, changeOrigin: true },
      },
    },
  }
})
