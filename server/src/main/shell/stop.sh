#!/usr/bin/env bash

FS_PID_FILE=/var/run/fs-server.pid

RUNNING_PID=`cat $FS_PID_FILE`
if [ $? == 0 ]; then
  echo "停止运行中的服务PID: $RUNNING_PID"
  kill -15 $RUNNING_PID
  rm -f $FS_PID_FILE
  exit 0
fi

echo "未找到$RUNNING_PID 服务可能没有启动"
exit 1