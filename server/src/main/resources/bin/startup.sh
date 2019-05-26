#!/usr/bin/env bash
java -version
if [ $? -ne 0 ]; then
    echo "No java command found in PATH"
    exit 0
fi

JAVA_VERSION=`java -version 2>&1 |awk 'NR==1{ gsub(/"/,""); print $3 }'`
echo "Java version: " $JAVA_VERSION

if [ "$1" == "-daemon" ]; then
    nohup java -Xbootclasspath/a:../conf -jar ../lib/flyingsocks-server-v1.0.jar >/dev/null  &
else
    java -Xbootclasspath/a:../conf -jar ../lib/flyingsocks-server-v1.0.jar
fi