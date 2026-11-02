import type { DataSource, DbConnectionMethod } from '../../api/types';

/** Database connection methods, in the order the edit page lists them (same labels as the API). */
export const METHODS: readonly { value: DbConnectionMethod; label: string; hint: string }[] = [
  {
    value: 'prism_database',
    label: 'Prism database (demo data)',
    hint: 'The STAR tables are in Prism’s own database server (e.g. the demo_star schema). No address or credentials.',
  },
  {
    value: 'firewall_allow_list',
    label: 'Firewall allow-list',
    hint: 'The client’s database accepts connections only from Prism’s fixed outgoing IP addresses.',
  },
  {
    value: 'private_link',
    label: 'Private link',
    hint: 'A private network link (AWS PrivateLink, Azure Private Link, Google PSC); no public address.',
  },
  {
    value: 'outbound_agent',
    label: 'Outbound agent',
    hint: 'A small connector inside the client’s network dials out to Prism; nothing inbound is opened.',
  },
  {
    // TEMPORARY (SQL/024): remove with the method once the secrets store exists.
    value: 'direct_dev',
    label: 'Direct (development only)',
    hint: 'Connects straight to a database with a username and password saved in Prism. For development databases only.',
  },
];

export function methodLabel(method: DbConnectionMethod | null): string {
  return METHODS.find((m) => m.value === method)?.label ?? '—';
}

/** "Prism database (demo data)", "Private link · db.acme.internal:5432/warehouse", "Client API · metrics.acme.com". */
export function connectionSummary(ds: DataSource): string {
  if (ds.connection_type === 'api') {
    try {
      return `Client API · ${new URL(ds.api_url ?? '').host}`;
    } catch {
      return 'Client API';
    }
  }
  if (ds.db_connection_method === 'prism_database' || !ds.db_host) return methodLabel(ds.db_connection_method);
  const port = ds.db_port ? `:${ds.db_port}` : '';
  return `${methodLabel(ds.db_connection_method)} · ${ds.db_host}${port}/${ds.db_name ?? ''}`;
}

/** True when Prism can actually connect to this kind of source today (the Prism database and direct_dev so far). */
export function isReachable(ds: Pick<DataSource, 'connection_type' | 'db_connection_method'>): boolean {
  return (
    ds.connection_type === 'database' &&
    (ds.db_connection_method === 'prism_database' || ds.db_connection_method === 'direct_dev')
  );
}
