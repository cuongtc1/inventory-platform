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
| `API_PROXY_TARGET` | `http://localhost:8082` | Where the dev server forwards `/api` (product-service now, the api-gateway on 8080 from T3.5). Dev server only, never reaches the browser |
| `VITE_API_BASE_URL` | empty | Base URL of API calls in the browser; empty = same origin, through the proxy |

## How the browser reaches the API

Since T3.4 product-service needs a token, and login comes in T3.5: until then the catalog pages show a 401 error.

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
│   ├── products.ts       getProducts, getProduct, createProduct, updateProduct, deleteProduct
│   ├── categories.ts     the same five calls for categories, plus getCategoryOptions; one file per resource
│   └── suppliers.ts      the same five calls for suppliers, plus getSupplierOptions
├── types/                API types mirroring the backend DTOs (api.ts, category.ts, supplier.ts, product.ts) and ui.ts
├── utils/
│   ├── format.ts         formatDate, formatDateTime (Vietnam time), formatMoney (USD), toApiDate
│   ├── errors.ts         ApiError, toApiError, errorMessage, formFieldErrors (validation errors → form fields)
│   └── table.ts          Ant Design Table ↔ API paging and sorting (current 1 ↔ page 0, ascend ↔ asc)
├── constants/
│   ├── app.ts            time zone, currency, page sizes, mobile breakpoint, sidebar widths
│   ├── routes.ts         every path (ROUTES) and route patterns
│   ├── navigation.ts     the sidebar menu: sections, entries, icons (NAVIGATION), activeNavPath
│   ├── validation.ts     form rules mirroring the backend (SKU pattern, lengths, money limits)
│   └── productStatus.ts  enum labels and badge tones (PRODUCT_STATUS, PRODUCT_STATUS_OPTIONS)
├── messages/texts.ts     every text the UI shows
├── theme/theme.ts        mockup colours as Ant Design tokens; badge tones
├── styles/global.css     the only global CSS
├── components/           shared components: StatusBadge, PageHeader (title row + tab title), PageLoading,
│                         EditButton, DeleteButton (table row actions), RecordFormPage (frame of every create/edit
│                         page), FormActions (Save changes / Cancel), AuditInfo (created/updated line)
├── layouts/              AppLayout (the frame around pages), AppHeader (top bar), SidebarMenu, Logo
├── pages/
│   ├── dashboard/        DashboardPage (backend connection check, catalog links)
│   ├── products/         ProductListPage (filters, sorting, paging, Edit/Delete), ProductFormPage (create and edit)
│   ├── categories/       CategoryListPage, CategoryFormPage
│   ├── suppliers/        SupplierListPage, SupplierFormPage
│   └── ComingSoonPage, NotFoundPage, RouteErrorPage
└── hooks/                useIsMobile (screen width), useStoredState (useState kept in localStorage),
                          usePagedList (list state in the URL), useSelectOptions, useReturnTo (form → back to the list)
```

## Catalog pages

| Screen | What you can do |
|---|---|
| `/products` | Search by name or SKU (Enter), filter by category, supplier and status, sort by clicking a column header, change page and page size. The SKU and **Edit** open the form; **Delete** asks first |
| `/products/new`, `/products/:id/edit` | Two-column form (one column on phones). Required fields and simple rules are checked in the browser; the backend's errors (e.g. "SKU already exists") appear under the field. Save and Cancel return to the list you came from |
| `/categories` | Search, sort by name or parent, paging. The product count opens the products of that category. Deleting a category that still has products or subcategories is refused with the backend's reason |
| `/categories/new`, `/categories/:id/edit` | Name and parent ("None (top level)" when cleared). A category isn't offered as its own parent; moving it under one of its subcategories shows the backend's error under Parent |
| `/suppliers` | Search, sort by any column, email opens the mail program. Deleting a supplier that still has products is refused |
| `/suppliers/new`, `/suppliers/:id/edit` | Name (required), contact, email, phone; an emptied field is saved as "not set" |

The list's state is in the URL, e.g. `/products?search=cable&status=ACTIVE&sort=salePrice,desc&page=2`: reload, Back and shared links keep it. The rules for building more list and form pages are in [docs/conventions.md](../docs/conventions.md) section 10 ("List pages", "Forms").

## Layout and screen sizes

Every page except login sits inside [AppLayout](src/layouts/AppLayout.tsx), which is built like the mockup: a dark sidebar menu, a white top bar (menu button, notifications bell, avatar, Log out) and the light content area. The layout changes with the screen width:

| Width | Menu | Top bar |
|---|---|---|
| ≥ 992 px (laptop, desktop) | Sidebar on the left. The top bar's button collapses it to icons (70 px) and back; the browser remembers the choice | Bell, avatar with name, "Log out" button |
| < 992 px (phone, tablet) | No sidebar. The ☰ button opens the same menu as a panel sliding in from the left; choosing an entry closes it | Icons only, smaller paddings |

- The breakpoint is `MOBILE_MAX_WIDTH` in `constants/app.ts`; components ask `useIsMobile()` instead of writing their own media query.
- The menu entries come from `NAVIGATION` in `constants/navigation.ts`; a new screen gets its menu entry there, not in the layout.
- Not built yet: the mockup's search box, the "View as" role switch (T3.5) and the unread count on the bell (Phase 8). Log out only goes to the login page until Phase 3.

To try the phone layout on a computer: browser DevTools (F12) → device toolbar (Ctrl+Shift+M) → pick a phone.

## Routing

[App.tsx](src/App.tsx) maps each path from [constants/routes.ts](src/constants/routes.ts) to a page:

| Path | Page |
|---|---|
| `/` | Dashboard |
| `/products`, `/products/new`, `/products/:id/edit` | Product list and form |
| `/categories`, `/categories/new`, `/categories/:id/edit` | Category list and form |
| `/suppliers`, `/suppliers/new`, `/suppliers/:id/edit` | Supplier list and form |
| `/warehouses`, `/stock-levels`, `/stock-movements`, `/low-stock`, `/customers`, `/sales-orders`, `/purchase-orders`, `/notifications`, `/reports`, `/users`, `/login` | "Coming soon" with the phase that builds them |
| anything else | 404 page |

- **Lazy pages:** each Phase 2 page is downloaded only when it is first opened (`lazy: { Component: () => import(...) }`), so the first download stays small and every page is its own file in `dist/`. Libraries go into named files (`vendor-react`, `vendor-antd`, `vendor-icons`, `vendor`) that browsers can keep cached across our releases.
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

**Router hooks.** `useParams()` reads parts of the URL (`:id` in `/products/:id/edit`, see `ProductFormPage`), `useNavigation()` tells whether the next page is still loading (`AppLayout` shows a spinner), and `<Link to={ROUTES.products}>` changes the page without reloading the browser. `useSearchParams()` reads and changes the `?search=...&page=2` part of the URL like a `useState` (see `usePagedList`), and a link's `state` travels with the history entry without showing in the URL (see `useReturnTo`).

**Forms.** Ant Design's `Form` keeps the field values itself; the page only gets them in `onFinish(values)`, already checked against the `rules`. `Form.useForm<ProductRequest>()` types those values, and `form.setFields(...)` puts the backend's errors under the fields.

**Custom hooks.** A function whose name starts with `use` and that calls other hooks; it lets components share logic, not markup. [useStoredState](src/hooks/useStoredState.ts) is `useState` that also saves the value in `localStorage` (the sidebar remembers being collapsed). [useIsMobile](src/hooks/useIsMobile.ts) uses `useSyncExternalStore`, React's hook for reading something outside React (here the browser's `matchMedia`): the component re-renders when the screen crosses the breakpoint, and the first render already has the right answer, so a phone never flashes the desktop layout.

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
