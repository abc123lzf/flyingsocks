@echo off
title "flyingsocks Server"

if "%1%" == "-install" (
    md C:\ProgramData\flyingsocks-server
    md C:\ProgramData\flyingsocks-server\log
    exit
)

echo "Run flyingsocks-server..."
if "%1%" == "-daemon" (
    start /b java -server -Xbootclasspath/a:../conf;../ -cp ../lib/flyingsocks-server-2.0.jar
) else (
    java -server -Xbootclasspath/a:../conf;../ -cp ../lib/flyingsocks-server-2.0.jar
)
echo "Complete."
pause