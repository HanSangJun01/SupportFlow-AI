@echo off
setlocal
set "BASE_DIR=%~dp0"
set "PROPS=%BASE_DIR%.mvn\wrapper\maven-wrapper.properties"
for /f "tokens=2 delims==" %%A in ('findstr /b "distributionUrl=" "%PROPS%"') do set "DIST_URL=%%A"
for %%A in ("%DIST_URL%") do set "DIST_FILE=%%~nxA"
set "DIST_NAME=%DIST_FILE:.zip=%"
set "MAVEN_DIR=%DIST_NAME:-bin=%"
if "%MAVEN_USER_HOME%"=="" set "MAVEN_USER_HOME=%USERPROFILE%\.m2"
set "DIST_DIR=%MAVEN_USER_HOME%\wrapper\dists\%DIST_NAME%"
set "MAVEN_BIN=%DIST_DIR%\%MAVEN_DIR%\bin\mvn.cmd"
if not exist "%MAVEN_BIN%" (
  mkdir "%DIST_DIR%" 2>nul
  powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -UseBasicParsing '%DIST_URL%' -OutFile '%DIST_DIR%\%DIST_FILE%'; Expand-Archive -Force '%DIST_DIR%\%DIST_FILE%' '%DIST_DIR%'"
)
call "%MAVEN_BIN%" %*
