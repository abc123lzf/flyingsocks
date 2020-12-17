#!/usr/bin/env bash

FUNCTION_RESULT=""

function getConfigDir() {
  CurrentDir=`pwd`
  cd ../config || exit 1
  ConfigDir=`pwd`
  cd $CurrentDir || exit 1
  FUNCTION_RESULT=$ConfigDir
  return 0
}

function getLibJars() {
  Result=""
  Prefix="../lib/"
  # shellcheck disable=SC2045
  for JarName in `ls ../lib`
  do
    Result="$Result $Prefix$JarName"
  done
  FUNCTION_RESULT=$Result
  return 0
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
CONFIG_DIR=$FUNCTION_RESULT

getLibJars
LIB_JARS=$FUNCTION_RESULT

if [ "$1" == "-t" ]; then
  java -server -Dflyingsocks.config.location="$CONFIG_DIR" -Xbootclasspath/a:../conf:../ -cp $LIB_JARS com.lzf.flyingsocks.server.ServerBoot
  exit 0
fi

nohup java -server -Dflyingsocks.config.location="$CONFIG_DIR" -Xbootclasspath/a:../conf:../ -cp $LIB_JARS com.lzf.flyingsocks.server.ServerBoot > /dev/null 2>&1& echo $! > .pid
exit 0