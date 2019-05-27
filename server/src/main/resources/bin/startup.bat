@echo off
if "%1%" == "-install" (
    md C:\ProgramData\flyingsocks-server
    md C:\ProgramData\flyingsocks-server\log
    exit
)

echo "Run flyingsocks-server..."
if "%1%" == "-daemon" (
    start /b java -Xbootclasspath/a:../conf -jar ../lib/flyingsocks-server-v1.0.jar
) else (
    java -Xbootclasspath/a:../conf -jar ../lib/flyingsocks-server-v1.0.jar
)
echo "Complete."
pause