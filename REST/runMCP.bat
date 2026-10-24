@echo off
rem Runs the API with the MCP server (Prism Advisor) on. See run.bat for the database password.
call "%~dp0run.bat" --prism.mcp.enabled=true %*
