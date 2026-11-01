@echo off
rem Runs the packaged API (build it first with build.bat). Extra arguments go to Spring Boot,
rem e.g. run.bat --prism.mcp.enabled=true
rem Passwords come from the environment: Prism's own login (PRISM_DB_PASSWORD or PGPASSWORD) and the read-only login
rem chart queries run on (PRISM_READER_PASSWORD, SQL/022). To avoid typing them, put
rem   PGPASSWORD=your-password
rem   PRISM_READER_PASSWORD=prism-reader-password
rem in REST\.env.local (git-ignored, never committed; the same file run.sh reads).
setlocal
cd /d "%~dp0"
if exist .env.local (
  for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env.local") do set "%%A=%%B"
)
java -jar target\prism-api-1.0.0.jar %*
