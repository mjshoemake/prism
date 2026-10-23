#!/usr/bin/env bash
# Runs the packaged API. Extra arguments are passed to Spring Boot, e.g. ./run.sh --prism.mcp.enabled=true
java -jar target/prism-api-1.0.0.jar "$@"
