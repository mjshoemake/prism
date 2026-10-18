#!/usr/bin/env bash
# Builds the React frontend into src/main/resources/static, then packages the Spring Boot jar.
set -euo pipefail
cd "$(dirname "$0")"

(cd frontend && npm run build)
mvn clean package
