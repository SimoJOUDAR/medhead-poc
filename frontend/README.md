# MedHead PoC — Frontend

React 19 + TypeScript (strict) + Vite module of the MedHead PoC. A single-page UI over the emergency recommendation API: JWT login, specialty selection, patient location input, and the recommendation result card. Project context, quick start, and the CI pipeline are documented in the [root `readme.md`](../readme.md).

## Scripts

```bash
npm ci                 # install dependencies
npm run dev            # dev server on http://localhost:5173
npm run lint           # ESLint (flat config, react-hooks rules)
npm test               # Vitest + React Testing Library + jest-axe
npm run test:watch     # same, in watch mode
npm run test:coverage  # same, with v8 coverage report at coverage/index.html
npm run build          # type-check + production bundle in dist/
npm run preview        # serve the production bundle locally

```

The dev server proxies `/api/*` to the backend on `http://localhost:8080` (see `vite.config.ts`), so no CORS configuration is needed in development. Boot the backend first — see the [root Quick Start](../readme.md#quick-start).

## Layout

- `src/auth/` — JWT login flow, `sessionStorage` persistence, typed `apiClient` + `ApiError`.
- `src/recommend/` — recommendation form, specialty dropdown, location inputs, result card, error mapping.
- `src/__tests__/` — WCAG 2.1 AA accessibility specs (jest-axe); see the [Accessibility section](../readme.md#accessibility) of the root readme.
