@echo off
setlocal

set BASE_DIR=%~dp0
set MAVEN_VERSION=3.9.11
set MAVEN_USER_HOME=%USERPROFILE%\.m2
set MVNW_DIST=%MAVEN_USER_HOME%\wrapper\dists\apache-maven-%MAVEN_VERSION%
set MVNW_HOME=%MVNW_DIST%\apache-maven-%MAVEN_VERSION%
set MVNW_ARCHIVE=%MVNW_DIST%\apache-maven-%MAVEN_VERSION%-bin.tar.gz
set MVNW_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.tar.gz

if not exist "%MVNW_HOME%\bin\mvn.cmd" (
  if not exist "%MVNW_DIST%" mkdir "%MVNW_DIST%"
  if not exist "%MVNW_ARCHIVE%" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%MVNW_URL%' -OutFile '%MVNW_ARCHIVE%'"
    if errorlevel 1 exit /b 1
  )
  tar -xzf "%MVNW_ARCHIVE%" -C "%MVNW_DIST%"
  if errorlevel 1 exit /b 1
)

"%MVNW_HOME%\bin\mvn.cmd" %*
