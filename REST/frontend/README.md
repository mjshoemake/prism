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

The UI is then at `http://localhost:8080/`. Routes use the URL hash (`/#/analysis`, `/#/dashboards`, `/#/users`, …)
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
│   ├── dashboardsApi.ts     /api/dashboards/* (user from X-User-ID until sessions, roadmap #4)
│   ├── dataSourcesApi.ts    /api/data-sources/* (enterprise from ?enterprisePk until sessions); options()
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
│   ├── analysis/
│   │   ├── AnalysisPage.tsx         Page (/#/analysis?dashboard=N): a saved dashboard's 1, 2 or 4 charts with shared
│   │   │                            drill-down; creates "My dashboard" for a user with none
│   │   ├── lastDashboard.ts         Last opened dashboard, remembered in localStorage ("prism_last_dashboard")
│   │   └── components/              AnalysisSidebar (dashboard picker + Edit, focus area/level, filters)
│   ├── dataSources/                 Metrics → Data Sources
│   │   ├── DataSourceListPage.tsx   Page (/#/data-sources): list with counts, Test (modal) / Edit / Delete, New
│   │   ├── DataSourceEditPage.tsx   Page (/#/data-sources/new, /#/data-sources/N/edit): name, description, connection
│   │   │                            (database method + host/port/db/secret name, or client API URL + secret name;
│   │   │                            TEMPORARY "Direct (development only)": username + write-only password, offered
│   │   │                            while GET /api/data-sources/options allows it; other saved settings kept)
│   │   ├── useDataSources.ts        list / reload / remove
│   │   ├── labels.ts                connection method labels and hints, connection summary
│   │   └── components/              ConnectionTestPanel (Test connection result, one line per table)
│   ├── dashboards/
│   │   ├── DashboardListPage.tsx    Page (/#/dashboards): list with Open / Edit / Delete, New dashboard
│   │   ├── DashboardEditPage.tsx    Page (/#/dashboards/new, /#/dashboards/N/edit[?from=analysis]): name, layout,
│   │   │                            metric + y-axis scale per chart area; Save/Cancel return where the user came from
│   │   ├── useDashboards.ts         The user's dashboards: list / create / update / remove (no implicit saves)
│   │   ├── layouts.ts               Layouts, visible positions, area labels, chart area per position
│   │   └── components/              DashboardChartCard (header label + chart; no metric picker)
│   ├── metrics/
│   │   ├── useMetricsExplorer.ts    All drill-down state and data loading for one or more charts (IDs only)
│   │   ├── useMetricList.ts         Metrics from GET /api/metrics/list
│   │   ├── metricFormat.ts          Value/tick/legend formatting; effectiveScale (y-axis from the chart's scale or the
│   │   │                            metric's default), clipToScale (hard custom bounds)
│   │   ├── filters.ts               Filter type, (blank) label, request filters
│   │   └── components/              ActiveFilters, MetricBarChart (incl. the barber-pole plugin)
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

The Analysis page shows one saved dashboard; every chart area has its own metric and y-axis scale (set on the Edit
page), and all charts share the focus area, level and filters.

1. Load the metrics (`GET /api/metrics/list`) and the user's dashboards; open the one in the URL, else the last
   opened, else the first. Load focus areas; select the first.
2. Load that area's focus levels; select the level last used in that area, else the first.
3. For each visible chart, `POST /api/metrics/data` with `{metric_pk, focus_level_pk, filters}` — IDs and values only. Rows come back as `[{key, label, value}]`: the label is shown on the bar; the key identifies it (labels may repeat, e.g. "Sprint 1" of two years).
4. Clicking a bar adds the filter `<level> = <bar label>` and moves to the level's drill target (`drill_to_focus_level_pk`, from the API). A level without one only adds the filter. An area can hold several drill paths (e.g. Year → … → Year & Sprint, and Quarter → Month → Sprint); the order levels are listed in is for display only. A click on any chart filters and drills every chart. Changing the dashboard, the level or the filters re-runs step 3. Filters are kept when the metric or the area changes.

### Y-axis scale

Set per chart area on the Edit page (`Dashboard_Charts.y_axis_mode`, SQL/021): **Metric default**, **0 to 100**,
**Fit to data** or **Custom** (minimum and/or maximum). Only Custom bounds are hard: a bar above the maximum is drawn to
the top with a striped (barber-pole) end, a bar below a raised minimum as a short striped stub, and the tooltip shows
the real value; a minimum above 0 shows "Axis starts at …". 0 to 100 stretches above 100 when a value exceeds it.
Scaling is visual only.

### Adding a metric

No UI change: insert a row into the `Metrics` table (type, columns, title, unit, decimals, scale — see `REST/CLAUDE.md`). It appears in the Edit page's metric lists within 15 minutes (the server's metadata cache) or after a restart.

## Known issues / TODO

- No real authentication: the session is just the stored login response.
