@echo off

if "%JAVA_OPTS%" == "" goto DEFAULT_JAVA_OPTS

:INVOKE
echo JAVA_HOME=%JAVA_HOME%
echo JAVA_OPTS=%JAVA_OPTS%
echo MICA_SERVER_HOME=%MICA_SERVER_HOME%

if "%MICA_SERVER_HOME%" == "" goto MICA_SERVER_HOME_NOT_SET

setlocal ENABLEDELAYEDEXPANSION

set MICA_SERVER_DIST=%~dp0..
echo MICA_SERVER_DIST=%MICA_SERVER_DIST%

set MICA_SERVER_LOG=%MICA_SERVER_HOME%\logs
IF NOT EXIST "%MICA_SERVER_LOG%" mkdir "%MICA_SERVER_LOG%"
echo MICA_SERVER_LOG=%MICA_SERVER_LOG%

rem Java 7 supports wildcard classpaths
rem http://docs.oracle.com/javase/7/docs/technotes/tools/windows/classpath.html
set CLASSPATH=%MICA_SERVER_HOME%\conf;%MICA_SERVER_DIST%\lib\*

set JAVA_DEBUG=-agentlib:jdwp=transport=dt_socket,server=y,address=8000,suspend=n

rem Add %JAVA_DEBUG% to this line to enable remote JVM debugging (for developers)
java %JAVA_OPTS% -cp "%CLASSPATH%" -DMICA_SERVER_HOME="%MICA_SERVER_HOME%" -DMICA_SERVER_DIST=%MICA_SERVER_DIST% org.obiba.mica.Application %*
goto :END

:DEFAULT_JAVA_OPTS
set JAVA_OPTS=-Xms1G -Xmx2G -XX:MaxPermSize=256M -XX:+UseG1GC
goto :INVOKE

:JAVA_HOME_NOT_SET
echo JAVA_HOME not set
goto :END

:MICA_SERVER_HOME_NOT_SET
echo MICA_SERVER_HOME not set
goto :END

:END
