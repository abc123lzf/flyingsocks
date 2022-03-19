set "JAVA=%JAVA_HOME%\bin\java.exe"
setlocal
set BASE_DIR=%~dp0
set BASE_DIR=%BASE_DIR:~0,-1%
for %%d in (%BASE_DIR%) do set BASE_DIR=%%~dpd
set CLASSPATH=.;%BASE_DIR%conf;%CLASSPATH%;%BASE_DIR%lib\*
cd ..
set FS_SERVER_HOME=%cd%
set "JAVA_OPT=%JAVA_OPT% -cp "%CLASSPATH%""
set "JAVA_OPT=%JAVA_OPT% -Dflyingsocks.config.location=%BASE_DIR%conf"
call %JAVA% %JAVA_OPT% %* com.lzf.flyingsocks.server.ServerBoot %*