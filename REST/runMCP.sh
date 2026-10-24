#!/usr/bin/env bash
# Runs the API with the MCP server (Prism Advisor) on. See run.sh for the database password.
exec "$(dirname "$0")/run.sh" --prism.mcp.enabled=true "$@"
