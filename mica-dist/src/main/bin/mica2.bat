@echo off

if "%JAVA_OPTS%" == "" goto DEFAULT_JAVA_OPTS

:INVOKE
echo JAVA_HOME=%JAVA_HOME%
echo JAVA_OPTS=%JAVA_OPTS%
echo MICA_HOME=%MICA_HOME%

if "%MICA_HOME%" == "" goto MICA_HOME_NOT_SET

setlocal ENABLEDELAYEDEXPANSION

set MICA_DIST=%~dp0..
echo MICA_DIST=%MICA_DIST%

set MICA_LOG=%MICA_HOME%\logs
IF NOT EXIST "%MICA_LOG%" mkdir "%MICA_LOG%"
echo MICA_LOG=%MICA_LOG%

set CLASSPATH=%MICA_HOME%\conf;%MICA_DIST%\webapp\WEB-INF\classes;%MICA_DIST%\webapp\WEB-INF\lib\*

set JAVA_DEBUG=-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n

rem Add %JAVA_DEBUG% to this line to enable remote JVM debugging (for developers)
java %JAVA_OPTS% -cp "%CLASSPATH%" -DMICA_HOME="%MICA_HOME%" -DMICA_DIST=%MICA_DIST% org.obiba.mica.Application %*
goto :END

:DEFAULT_JAVA_OPTS
set JAVA_OPTS=-Xms1G -Xmx2G -XX:+UseG1GC
goto :INVOKE

:JAVA_HOME_NOT_SET
echo JAVA_HOME not set
goto :END

:MICA_HOME_NOT_SET
echo MICA_HOME not set
goto :END

:END
