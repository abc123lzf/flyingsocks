#!/usr/bin/env bash

rm -rf target
mvn clean install -pl common -Dmaven.test.skip=true
mvn -Dmaven.test.skip=true clean package install -pl server assembly:single

