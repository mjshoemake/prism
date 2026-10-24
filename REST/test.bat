@echo off
rem Builds and runs the tests (mvn test). ApiApplicationTests starts the application, so PostgreSQL must be running;
rem the password comes from REST\.env.local or the environment, as for run.bat.
rem Build and run the app with: build.bat, then run.bat.
rem The metric parity check: from Git Bash, PGPASSWORD=... bash tools/parity/run.sh
setlocal
cd /d "%~dp0"
if exist .env.local (
  for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env.local") do set "%%A=%%B"
)
mvn test
