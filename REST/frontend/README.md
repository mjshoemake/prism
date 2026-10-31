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
│   │   ├── useMetricsExplorer.ts    All drill-down state and data loading (requests by IDs only)
│   │   ├── useMetricList.ts         Metrics from GET /api/metrics/list + the selected one
│   │   ├── metricFormat.ts          Value/tick/legend formatting and y-axis bounds from the metric's metadata
│   │   ├── filters.ts               Filter type, (blank) label, request filters
│   │   └── components/              SelectionsPanel, ActiveFilters, ChartPanel, MetricBarChart
│   ├── dashboards/
│   │   ├── DashboardsPage.tsx       Page (/#/dashboards): sidebar + 1, 2 or 4 chart areas
│   │   ├── useDashboards.ts         The user's saved dashboards; every change saved at once (undone on failure)
│   │   ├── layouts.ts               Layouts, visible positions, metric per position
│   │   └── components/              DashboardSidebar (picker, new/rename/delete, layout, shared selections),
│   │                                DashboardChartCard (metric picker + chart)
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

1. Load the metrics (`GET /api/metrics/list`); select the first. Load focus areas; select the first.
2. Load that area's focus levels; select the level last used in that area, else the first.
3. `POST /api/metrics/data` with `{metric_pk, focus_level_pk, filters}` — IDs and values only. Rows come back as `[{key, label, value}]`: the label is shown on the bar; the key identifies it (labels may repeat, e.g. "Sprint 1" of two years).
4. Clicking a bar adds the filter `<level> = <bar label>` and moves to the level's drill target (`drill_to_focus_level_pk`, from the API). A level without one only adds the filter. An area can hold several drill paths (e.g. Year → … → Year & Sprint, and Quarter → Month → Sprint); the order levels are listed in is for display only. Changing the metric, the level or the filters re-runs step 3. Filters are kept when the metric or the area changes.

### Adding a metric

No UI change: insert a row into the `Metrics` table (type, columns, title, unit, decimals, scale — see `REST/CLAUDE.md`). It appears in the Metric picker within 15 minutes (the server's metadata cache) or after a restart.

## Known issues / TODO

- No real authentication: the session is just the stored login response.
