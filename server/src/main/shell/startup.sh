#!/usr/bin/env bash

function getConfigDir() {
  CurrentDir=`pwd`
  cd ../config || exit 1
  ConfigDir=`pwd`
  cd $CurrentDir || exit 1
  return $ConfigDir
}

function getLibJars() {
  Result=""
  Prefix="../lib/"
  # shellcheck disable=SC2045
  for JarName in `ls ../lib`
  do
    Result="$Result $Prefix$JarName"
  done
  return $Result
}

function checkoutRunningProcess() {
  RUNNING_PID=`cat .pid`
  if [ $? == 0 ]; then
    echo "停止运行中的服务PID: $RUNNING_PID"
    kill -15 $RUNNING_PID
    rm -f .pid
  fi
  return 0
}

checkoutRunningProcess
getConfigDir
CONFIG_DIR=$?

getLibJars
LIB_JARS=$?

if [ "$1" == "-t" ]; then
  java -server -Dflyingsocks.config.location=$CONFIG_DIR -Xbootclasspath/a:../conf:../ -cp $LIB_JARS com.lzf.flyingsocks.server.ServerBoot
  exit 0
fi

nohup java -server -Dflyingsocks.config.location=$CONFIG_DIR -Xbootclasspath/a:../conf:../ -cp $LIB_JARS com.lzf.flyingsocks.server.ServerBoot > /dev/null 2>&1& echo $! > .pid
exit 0