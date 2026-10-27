# Stockline frontend

React + Vite + TypeScript (strict) + Ant Design + React Router + Axios. Rules: [docs/conventions.md](../docs/conventions.md#10-frontend) section 10. UI reference: [docs/mockup/](../docs/mockup/).

## Run

Needs Node 20+ with **npm 10 or newer** (`npm -v`). The backend must run for API calls: product-service on `localhost:8082` (see the root README).

```bash
cd frontend
npm install          # once, and after package.json changes
npm run dev          # http://localhost:5173 ; /api is forwarded to the backend
npm run lint         # ESLint
npm run build        # type check (tsc) + production build into dist/
```

Settings (optional): copy `.env.example` to `.env`.

| Variable | Default | Purpose |
|---|---|---|
| `API_PROXY_TARGET` | `http://localhost:8082` | Where the dev server forwards `/api` (product-service now, the api-gateway on 8080 from Phase 3). Dev server only, never reaches the browser |
| `VITE_API_BASE_URL` | empty | Base URL of API calls in the browser; empty = same origin, through the proxy |

## How the browser reaches the API

```
browser ──/api/products──▶ Vite dev server :5173 ──proxy──▶ product-service :8082
```

The browser only talks to `localhost:5173`, so there is no cross-origin request and the services need no CORS settings. From Phase 3 the proxy target becomes the api-gateway, which handles CORS for deployments.

## Folder structure

```
src/
├── main.tsx              entry point: Ant Design theme and App wrapper around <App />
├── App.tsx               the route table (React Router), lazy-loaded pages
├── api/
│   ├── client.ts         the one Axios instance; every failed call becomes an ApiError
│   └── categories.ts     getCategories (T2.4 adds the rest); one file per resource
├── types/                API types mirroring the backend DTOs (api.ts, category.ts, supplier.ts, product.ts) and ui.ts
├── utils/
│   ├── format.ts         formatDate, formatDateTime (Vietnam time), formatMoney (USD), toApiDate
│   ├── errors.ts         ApiError, toApiError, errorMessage, formFieldErrors (validation errors → form fields)
│   └── table.ts          Ant Design Table ↔ API paging and sorting (current 1 ↔ page 0, ascend ↔ asc)
├── constants/
│   ├── app.ts            time zone, currency, page sizes
│   ├── routes.ts         every path (ROUTES) and route patterns
│   └── productStatus.ts  enum labels and badge tones (PRODUCT_STATUS, PRODUCT_STATUS_OPTIONS)
├── messages/texts.ts     every text the UI shows
├── theme/theme.ts        mockup colours as Ant Design tokens; badge tones
├── styles/global.css     the only global CSS
├── components/           shared components: StatusBadge, PageHeader (title row + tab title), PageLoading
├── layouts/AppLayout.tsx the frame around pages (content area now; sidebar and top bar in T2.2)
├── pages/
│   ├── dashboard/        DashboardPage (backend connection check, catalog links)
│   ├── products/         ProductListPage, ProductFormPage (placeholders until T2.3)
│   ├── categories/       CategoryListPage (placeholder until T2.4)
│   ├── suppliers/        SupplierListPage (placeholder until T2.4)
│   └── ComingSoonPage, NotFoundPage, RouteErrorPage
└── hooks/                reusable hooks
```

## Routing

[App.tsx](src/App.tsx) maps each path from [constants/routes.ts](src/constants/routes.ts) to a page:

| Path | Page |
|---|---|
| `/` | Dashboard |
| `/products`, `/products/new`, `/products/:id/edit` | Product list and form (T2.3) |
| `/categories`, `/suppliers` | Category and supplier pages (T2.4) |
| `/warehouses`, `/stock-levels`, `/stock-movements`, `/low-stock`, `/customers`, `/sales-orders`, `/purchase-orders`, `/notifications`, `/reports`, `/users`, `/login` | "Coming soon" with the phase that builds them |
| anything else | 404 page |

- **Lazy pages:** each Phase 2 page is downloaded only when it is first opened (`lazy: { Component: () => import(...) }`), so the first download stays small and every page is its own file in `dist/`. Libraries go into named files (`vendor-react`, `vendor-antd`, `vendor`) that browsers can keep cached across our releases.
- **Errors:** a page that crashes shows `RouteErrorPage` instead of a blank screen; the rest of the app keeps working.
- **Tab titles:** `PageHeader` renders a `<title>` ("Products · Stockline"); React 19 moves it into `<head>` by itself.

## React basics used here

A short guide, with examples from this code.

**Components.** A component is a function that returns what to show, written in JSX (HTML-like syntax inside TypeScript). Its name starts with a capital letter and it's used like a tag: `<PageHeader title="Products" />`. Components are put together like building blocks: `App` → `AppLayout` → `ProductListPage` → `PageHeader`.

```tsx
export function ProductListPage() {
  return (
    <>
      <PageHeader title={texts.product.title} extra={<Button type="primary">{texts.product.newButton}</Button>} />
      <Empty description={texts.pages.placeholder('T2.3')} />
    </>
  )
}
```

`{ ... }` inside JSX runs TypeScript (here: reads a text). `<> ... </>` groups several elements without adding a `<div>`.

**Props.** The inputs of a component, like method parameters: `PageHeader` receives `title` and `extra`. They are read-only; a component never changes its props. `StatusBadge<T>` shows that props can be generic and type-checked.

**State.** Data that belongs to a component and changes over time. `useState` returns the current value and a function to change it; changing it makes React call the component again ("re-render") with the new value. In [DashboardPage](src/pages/dashboard/DashboardPage.tsx):

```tsx
const [connection, setConnection] = useState<Connection>({ status: 'checking' })
// later: setConnection({ status: 'connected', count: 3 }) → React renders the page again, now with the green alert
```

Never change state by assigning to it (`connection.status = ...`); always call the setter.

**Effects.** Code that talks to the outside world (API calls, timers) doesn't belong in the render itself, because rendering can happen many times. `useEffect` runs it *after* rendering:

```tsx
useEffect(() => {
  const controller = new AbortController()
  getCategories({ page: 0, size: 1 }, controller.signal).then(page => setConnection(...))
  return () => controller.abort()   // cleanup: runs when the page is left
}, [])                              // [] = run once; [id] would run again whenever id changes
```

In development, `StrictMode` runs every effect twice (run, cleanup, run) on purpose, to show effects that don't clean up after themselves. That's why the request has a cleanup that cancels it.

**Router hooks.** `useParams()` reads parts of the URL (`:id` in `/products/:id/edit`, see `ProductFormPage`), `useNavigation()` tells whether the next page is still loading (`AppLayout` shows a spinner), and `<Link to={ROUTES.products}>` changes the page without reloading the browser.

Rules in short: pages call functions in `api/` (never Axios directly), show texts from `messages/texts.ts`, format with `utils/format.ts`, show enums with `StatusBadge` and a `constants/` map, take colours from the theme, and decide on errors by `ApiError.code`.

## New compared to a Java background

| Java / Spring | Here |
|---|---|
| A record `ProductResponse` | A TypeScript `interface ProductResponse` with the same fields (`types/product.ts`) |
| `enum ProductStatus` | A union type `'ACTIVE' \| 'INACTIVE'`; this tsconfig (`erasableSyntaxOnly`) doesn't allow TypeScript `enum` |
| `messages.properties` | `messages/texts.ts`, a typed object: a wrong key is a compile error |
| `DateTimeUtils`, `MoneyUtils` | `utils/format.ts` with the built-in `Intl` API, same formats |
| `GlobalExceptionHandler` | `api/client.ts` interceptor + `utils/errors.ts` |
| `@GetMapping("/products/{id}")` + `@PathVariable` | A route `/products/:id/edit` + `useParams()` |
| Sealed interface with records | A union of object types with a `status` field (`Connection` in DashboardPage) |
| A field that changes, then repaint by hand | `useState`: change it with the setter, React re-renders |
