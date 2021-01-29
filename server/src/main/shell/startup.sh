#!/usr/bin/env bash

if [ -z "$FS_HOME" ]; then
  export FS_HOME=/opt/flyingsocks-server
fi


FS_PID_FILE=/var/run/fs-server.pid
if [ -f $FS_PID_FILE ]; then
  RUNNING_PID=`cat $FS_PID_FILE`
  if [ $? == 0 ]; then
      echo "Stop flyingsocks server process [ PID: $RUNNING_PID ]"
      kill -15 $RUNNING_PID
      rm -f $FS_PID_FILE
  fi
fi

FS_CONFIG_DIR=$FS_HOME/config
FS_LIB_DIR=$FS_HOME/lib

if [ "$1" == "-t" ]; then
  java -server -Dflyingsocks.config.location="$FS_CONFIG_DIR" -Xbootclasspath/a:$FS_CONFIG_DIR:../ -cp "$FS_LIB_DIR/*" com.lzf.flyingsocks.server.ServerBoot
  exit 0
fi

nohup java -server -Dflyingsocks.config.location="$FS_CONFIG_DIR" -Xbootclasspath/a:$FS_CONFIG_DIR:../ -cp "$FS_LIB_DIR/*" com.lzf.flyingsocks.server.ServerBoot > /dev/null 2>&1& echo $! > $FS_PID_FILE
exit 0