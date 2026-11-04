import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

/**
 * Vite configuration (https://vite.dev/config/).
 *
 * Dev proxy (docs/conventions.md section 10): the browser calls "/api/..." on localhost:5173, and the Vite dev
 * server forwards it to the backend. So the browser only ever sees one origin, and the services need no CORS
 * settings. The target is the api-gateway, which needs the login token and passes each call to its service.
 * Change it with API_PROXY_TARGET in frontend/.env (see .env.example); it has no VITE_ prefix on purpose, so it
 * stays in the dev server and never reaches the browser.
 */
export default defineConfig(({ mode }) => {
  // loadEnv reads frontend/.env* for this mode; '' = all variables, not only VITE_*
  const env = loadEnv(mode, process.cwd(), '')
  const apiTarget = env.API_PROXY_TARGET || 'http://localhost:8080'

  return {
    plugins: [react()],
    build: {
      // Vite warns above 500 kB. vendor-antd alone is ~820 kB (~265 kB gzipped) once pages use Table, Form, Select
      // and friends; it is downloaded once and then cached, and splitting it further wouldn't make the total smaller.
      // The limit still catches a real problem, like a library accidentally landing in our own code.
      chunkSizeWarningLimit: 1000,
      rolldownOptions: {
        output: {
          // Libraries in their own files, by name: they change less often than our code, so browsers keep them
          // cached across our releases. Pages are split already, by the lazy routes in App.tsx.
          codeSplitting: {
            groups: [
              { name: 'vendor-react', test: /node_modules[\\/](react|react-dom|react-router|scheduler)[\\/]/, priority: 4 },
              { name: 'vendor-icons', test: /node_modules[\\/]@ant-design[\\/]icons(-svg)?[\\/]/, priority: 3 },
              { name: 'vendor-antd', test: /node_modules[\\/](antd|@ant-design|@rc-component|rc-[^\\/]+|dayjs)[\\/]/, priority: 2 },
              { name: 'vendor', test: /node_modules[\\/]/, priority: 1 },
            ],
          },
        },
      },
    },
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
