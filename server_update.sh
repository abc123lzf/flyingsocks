#!/usr/bin/env bash

# 文件目录
if [ -z "$FS_HOME" ]; then
  export FS_HOME=/opt/flyingsocks-server
fi

function backupConfig() {
  rm -rf /tmp/flyingsocks-backup
  mkdir /tmp/flyingsocks-backup
  cp $FS_HOME/config /tmp/flyingsocks-backup || return 1
  return 0
}

backupConfig

git pull
if [ $? -ne 0 ]; then
  echo "Git pull failure"
  exit 1
fi

mvn clean install -pl server -am -Dmaven.test.skip=true
if [ $? -ne 0 ]; then
  echo "Clean and install failure"
  exit 1
fi

cd server || exit 1

mvn assembly:single
if [ $? -ne 0 ]; then
  echo "Package failure"
  exit 1
fi

rm -rf $FS_HOME

cd ..
cp server/target/flyingsocks-server-bin.zip ..
unzip ../flyingsocks-server-bin.zip -d $FS_HOME
