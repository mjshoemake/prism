@echo off
rem Runs the packaged API (build it first with build.bat). Extra arguments go to Spring Boot,
rem e.g. run.bat --prism.mcp.enabled=true
rem The database password comes from the environment (PRISM_DB_PASSWORD or PGPASSWORD). To avoid typing it, put
rem   PGPASSWORD=your-password
rem in REST\.env.local (git-ignored, never committed; the same file run.sh reads).
setlocal
cd /d "%~dp0"
if exist .env.local (
  for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env.local") do set "%%A=%%B"
)
java -jar target\prism-api-1.0.0.jar %*
