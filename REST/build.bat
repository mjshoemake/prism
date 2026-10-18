@echo off
rem Builds the React frontend into src\main\resources\static, then packages the Spring Boot jar.
pushd "%~dp0frontend" || exit /b 1
call npm run build || (popd & exit /b 1)
popd
cd /d "%~dp0"
mvn clean package
