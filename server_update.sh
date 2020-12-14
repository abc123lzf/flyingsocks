#!/usr/bin/env bash

function backupConfig() {
  cp ../flyingsocks-server/config /tmp || return 1
  return 0
}

backupConfig

git pull
if [ $? -ne 0 ]; then
  echo "Git pull failure"
  exit 1
fi

mvn clean install
if [ $? -ne 0 ]; then
  echo "Clean and install failure"
  exit 1
fi

cd ../server || exit 1

mvn assembly:single
if [ $? -ne 0 ]; then
  echo "Package failure"
  exit 1
fi

cd ..
cp server/target/flyingsocks-server-bin.zip ..
unzip ../flyingsocks-server-bin.zip -d ..
