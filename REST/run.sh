#!/usr/bin/env bash
# Runs the packaged API (build it first with ./build.sh). Extra arguments go to Spring Boot,
# e.g. ./run.sh --prism.mcp.enabled=true
# The database password comes from the environment (PRISM_DB_PASSWORD or PGPASSWORD). To avoid typing it, put
#   PGPASSWORD=your-password
# in REST/.env.local (git-ignored, never committed).
set -euo pipefail
cd "$(dirname "$0")"
if [ -f .env.local ]; then
  set -a; . ./.env.local; set +a
fi
exec java -jar target/prism-api-1.0.0.jar "$@"
