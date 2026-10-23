#!/usr/bin/env bash
# Metric parity check (README.md): rebuilds the hand-checked fixture database, then runs MetricParityTests.
#   bash tools/parity/run.sh            compare with the hand-checked answers and the frozen baseline
#   bash tools/parity/run.sh --freeze   write a new baseline (only after a change has been accepted)
# Needs the demo database (playpen, SQL 001–012+) and psql; PGPASSWORD as for any psql call.
set -euo pipefail
here="$(cd "$(dirname "$0")" && pwd)"
rest="$(cd "$here/../.." && pwd)"

extra=""
case "${1:-}" in
  "") ;;
  --freeze) extra="-Dprism.parity.freeze=true" ;;
  *) echo "usage: $0 [--freeze]" >&2; exit 2 ;;
esac

bash "$here/fixture/fixture.sh"
cd "$rest"
if [ -x ./mvnw ]; then mvn=./mvnw; else mvn=mvn; fi
$mvn test -Dtest=MetricParityTests -Dprism.parity=true $extra
