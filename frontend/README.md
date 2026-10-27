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
├── main.tsx              entry point: Ant Design theme, App wrapper (router from T2.1)
├── App.tsx               placeholder start page until T2.1
├── api/client.ts         the one Axios instance; every failed call becomes an ApiError
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
├── components/           shared components: StatusBadge
├── layouts/              AppLayout (T2.2)
├── pages/                one folder per area (T2.3, T2.4)
└── hooks/                reusable hooks
```

Rules in short: pages call functions in `api/` (never Axios directly), show texts from `messages/texts.ts`, format with `utils/format.ts`, show enums with `StatusBadge` and a `constants/` map, take colours from the theme, and decide on errors by `ApiError.code`.

## New compared to a Java background

| Java / Spring | Here |
|---|---|
| A record `ProductResponse` | A TypeScript `interface ProductResponse` with the same fields (`types/product.ts`) |
| `enum ProductStatus` | A union type `'ACTIVE' \| 'INACTIVE'`; this tsconfig (`erasableSyntaxOnly`) doesn't allow TypeScript `enum` |
| `messages.properties` | `messages/texts.ts`, a typed object: a wrong key is a compile error |
| `DateTimeUtils`, `MoneyUtils` | `utils/format.ts` with the built-in `Intl` API, same formats |
| `GlobalExceptionHandler` | `api/client.ts` interceptor + `utils/errors.ts` |
