@echo off

set jarpath=../lib/flyingsocks-server-1.1.jar
set allparam=

:param
set str=%1
if "%str%"=="" (
    goto end
)
set allparam=%allparam% %str%
shift /0
goto param

:end
if "%allparam%"=="" (
    goto eof
)

rem remove left right blank
:intercept_left
if "%allparam:~0,1%"==" " set "allparam=%allparam:~1%"&goto intercept_left

:intercept_right
if "%allparam:~-1%"==" " set "allparam=%allparam:~0,-1%"&goto intercept_right

:eof
java -jar %jarpath% com.lzf.flyingsocks.server.tools.ConfigFileTool %allparam%

pause