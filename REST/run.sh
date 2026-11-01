#!/usr/bin/env bash
# Runs the packaged API (build it first with ./build.sh). Extra arguments go to Spring Boot,
# e.g. ./run.sh --prism.mcp.enabled=true
# Passwords come from the environment: Prism's own login (PRISM_DB_PASSWORD or PGPASSWORD) and the read-only login
# chart queries run on (PRISM_READER_PASSWORD, SQL/022). To avoid typing them, put
#   PGPASSWORD=your-password
#   PRISM_READER_PASSWORD=prism-reader-password
# in REST/.env.local (git-ignored, never committed).
set -euo pipefail
cd "$(dirname "$0")"
if [ -f .env.local ]; then
  set -a; . ./.env.local; set +a
fi
exec java -jar target/prism-api-1.0.0.jar "$@"
