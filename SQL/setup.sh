#!/usr/bin/env bash
# Sets up a NEW Prism database: creates it (if missing) and runs every migration script in order, 001 star schema
# … 012 metric types, plus any later NNN-*.sql. For a fresh database only: scripts 001–008 are not re-runnable, so it
# stops if the "prism" schema already exists (run new migrations one by one instead, see REST/CLAUDE.md).
#   bash SQL/setup.sh                    database "playpen" on localhost:5432 as postgres
#   PRISM_DB=mydb bash SQL/setup.sh      another database name
#   PRISM_PSQL_BASE="/path/to/psql -h host -p 5432 -U user" bash SQL/setup.sh
#   PGPASSWORD                            as for any psql call
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
DB=${PRISM_DB:-playpen}

find_psql() {
  # PATH first, then the copies bundled with Postgres.app and pgAdmin (macOS), which aren't on the PATH by default.
  for candidate in "$(command -v psql || true)" \
                   /Applications/Postgres.app/Contents/Versions/latest/bin/psql \
                   "/Applications/pgAdmin 4.app/Contents/SharedSupport/psql"; do
    if [ -n "$candidate" ] && [ -x "$candidate" ]; then printf '%s\n' "$candidate"; return; fi
  done
  echo "psql not found: add it to PATH or set PRISM_PSQL_BASE" >&2
  exit 1
}
if [ -n "${PRISM_PSQL_BASE:-}" ]; then
  read -r -a PSQL <<< "$PRISM_PSQL_BASE"
else
  PSQL=("$(find_psql)" -h localhost -p 5432 -U postgres)
fi
export PGOPTIONS='-c client_min_messages=warning'   # hide NOTICEs; warnings and errors still show

if [ -z "$("${PSQL[@]}" -d postgres -Atc "SELECT 1 FROM pg_database WHERE datname = '$DB'")" ]; then
  "${PSQL[@]}" -d postgres -q -v ON_ERROR_STOP=1 -c "CREATE DATABASE \"$DB\"" >/dev/null
  echo "created database $DB"
fi

if [ "$("${PSQL[@]}" -d "$DB" -Atc "SELECT 1 FROM pg_namespace WHERE nspname = 'prism'")" = "1" ]; then
  echo "database $DB already has the prism schema; setup.sh is for a new database only" >&2
  exit 1
fi

for script in "$here"/[0-9][0-9][0-9]-*.sql; do
  echo "run  $(basename "$script")"
  "${PSQL[@]}" -d "$DB" -q -v ON_ERROR_STOP=1 -f "$script" >/dev/null
done
echo "database $DB ready: $("${PSQL[@]}" -d "$DB" -Atc 'SELECT count(*) FROM prism."Sprint_Metrics_Fact"') demo fact rows,"\
     "$("${PSQL[@]}" -d "$DB" -Atc 'SELECT count(*) FROM prism."Metrics"') metrics"
