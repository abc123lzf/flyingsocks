#!/usr/bin/env bash

rm -rf target
if [ "$1" == "" ]; then
    echo "please input os version, optional versions: win64、win32、linux32 、linux64 and macos."
    exit 0
fi
mvn clean install -pl common -Dmaven.test.skip=true
mvn -Dmaven.test.skip=true clean package install -pl client -P $1 assembly:single