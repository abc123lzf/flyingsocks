#!/usr/bin/env bash

if [ "$1" == "-install" ]; then
    echo "make folder /var/log/flyingsocks-cli"
    mkdir /var/log/flyingsocks-cli
    echo "make folder /etc/flyingsocks-cli"
    mkdir /etc/flyingsocks-cli
    exit 0
fi

java -version
if [ $? -ne 0 ]; then
    echo "No java command found in PATH"
    exit 0
fi

JAVA_VERSION=`java -version 2>&1 |awk 'NR==1{ gsub(/"/,""); print $3 }'`
echo "Java version: " $JAVA_VERSION

if [ "$1" == "-daemon" ]; then
    nohup java -Xbootclasspath/a:../conf -jar ../lib/flyingsocks-cli-2.0.jar >/dev/null  &
else
    javaw -Xbootclasspath/a:../conf -jar ../lib/flyingsocks-cli-2.0.jar
fi