@echo off
title "flyingsocks Server"

if "%1%" == "-t" (
    java -server -Xbootclasspath/a:../conf;../ -cp ../lib/flyingsocks-server-3.0-SNAPSHOT.jar
) else (
    start /b java -server -Xbootclasspath/a:../conf;../ -cp ../lib/flyingsocks-server-3.0-SNAPSHOT.jar
)
echo "Complete."
pause