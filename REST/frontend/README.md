# Prism Web (frontend)

> **New to the codebase?** Read the [UI developer guide](docs/DEVELOPER_GUIDE.md) (how it works, adding pages,
> security) and the [product overview](../docs/OVERVIEW.md). This README is the quick reference.

React + TypeScript single-page app for Prism, built with [Vite](https://vite.dev).
Styling is Bootstrap 5 with the purple/black theme; charts use Chart.js via `react-chartjs-2`.

## Getting started

Requires Node.js 20+.

```bash
cd REST/frontend
npm install          # first time only
npm run dev          # http://localhost:5173 (hot reload)
```

The dev server proxies `/api/*` to the Spring Boot API on `http://localhost:8080`, so start the API too.

## Scripts

| Command | What it does |
|---|---|
| `npm run dev` | Dev server with hot reload and the `/api` proxy. |
| `npm run build` | Type-checks, then writes the production bundle to `../src/main/resources/static/` (**that folder is emptied first**). |
| `npm run typecheck` | TypeScript only. |
| `npm run lint` | ESLint (TypeScript + React hooks rules). |
| `npm run preview` | Serves the production build locally. |

### Shipping with the API

Spring Boot serves whatever is in `src/main/resources/static`, so the release order is:

```bash
(cd frontend && npm run build) && mvn clean package
```

The UI is then at `http://localhost:8080/`. Routes use the URL hash (`/#/metrics`, `/#/users`, …)
so Spring doesn't need a fallback route for client-side paths.

## Project layout

```
src/
├── main.tsx                 Entry point: global CSS + Bootstrap JS, renders <App/>
├── App.tsx                  Providers + route table
├── api/                     Everything that talks HTTP
│   ├── client.ts            fetch wrapper (JSON, ApiError, abort helpers)
│   ├── types.ts             Shapes of the API's JSON
│   ├── metricsApi.ts        /api/metrics/*
│   ├── usersApi.ts          /api/users/*
│   ├── settingsApi.ts       /api/settings
│   └── authApi.ts           /api/auth/*
├── auth/                    Session state
│   ├── AuthProvider.tsx     Holds the login response; persists it in localStorage ("prism_auth")
│   ├── useAuth.ts           Hook for components
│   └── RequireAuth.tsx      Route guard → /login
├── components/              Reusable, feature-agnostic UI
│   ├── layout/              AppLayout (navbar + page outlet), NavBar
│   ├── toast/               ToastProvider + useToast (bottom-right snackbar)
│   ├── DropdownSelect.tsx   Bootstrap dropdown that behaves like a <select>
│   ├── ErrorAlert.tsx
│   ├── LoadingSpinner.tsx
│   └── Modal.tsx            Accessible dialog used for add/edit forms
├── features/                One folder per screen/domain
│   ├── metrics/
│   │   ├── MetricsPage.tsx          Page: sidebar + chart
│   │   ├── useMetricsExplorer.ts    All drill-down state and data loading
│   │   ├── metricDefinitions.ts     Metrics that can be charted (Say/Do Ratio)
│   │   ├── filters.ts               Filter type + WHERE-clause builder
│   │   └── components/              SelectionsPanel, ActiveFilters, ChartPanel, MetricBarChart
│   ├── users/
│   │   ├── UsersPage.tsx
│   │   ├── useUsers.ts              List + CRUD with toasts
│   │   └── components/              UsersTable, UserRow, AddUserModal, EditUserModal, TemporaryPasswordNotice
│   ├── settings/                    SystemSettingsPage (Admin → System Settings), useSystemSettings, validHours
│   └── auth/                        LoginPage, ChangePasswordPage
├── theme/
│   ├── purple-black-theme.css       Bootstrap overrides (colours, cards, forms, dropdowns…)
│   └── palette.ts                   Same colours for Chart.js (keep in sync with the CSS)
└── styles/layout.css                Page layout (split view, narrow pages, toast position)
prototypes/                          Old experiments, not built (see prototypes/README.md)
```

### Conventions

- **Pages** live in `features/<domain>/`; a page composes components and gets its data from a hook in the same folder.
- **Hooks own state and API calls** (`useMetricsExplorer`, `useUsers`); components receive data and callbacks as props.
- **Only `src/api/` calls `fetch`.** Add a function to the matching `*Api.ts` and a type to `types.ts`.
- **User feedback** goes through `useToast().showToast(message, 'info' | 'success' | 'warning')`.
- Named exports for everything except `App`.

## How the metrics drill-down works

1. Load focus areas; select the first.
2. Load that area's focus levels; select the level last used in that area, else the first.
3. Query `/api/metrics/sprint-metrics` grouped by the selected level's `column_name`, with a WHERE clause built from the active filters.
4. Clicking a bar adds the filter `<level> = <bar label>` and moves to the next level. Changing the level or the filters re-runs step 3.

### Adding a metric

Add a `MetricDefinition` to `features/metrics/metricDefinitions.ts` (SQL alias `key`, `title`, `legendLabel`, axis `max`, tick formatter, and the `selectSql` fragment), then pass it to `useMetricsExplorer` / `ChartPanel` in `MetricsPage.tsx`.

## Known issues / TODO

- The client sends SQL fragments (`selectSql`, WHERE clause) to the API — see backlog in `REST/CLAUDE.md`.
- No real authentication: the session is just the stored login response.
