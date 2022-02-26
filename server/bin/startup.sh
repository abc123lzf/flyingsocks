#!/usr/bin/env bash

# find FS_HOME
if [ -z "$FS_SERVER_HOME" ] ; then
  ## resolve links - $0 may be a link to maven's home
  PRG="$0"

  # need this for relative symlinks
  while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
      PRG="$link"
    else
      PRG="`dirname "$PRG"`/$link"
    fi
  done

  saveddir=`pwd`
  FS_SERVER_HOME=`dirname "$PRG"`/..
  # make it fully qualified
  FS_SERVER_HOME=`cd "$FS_SERVER_HOME" && pwd`
  cd "$saveddir"
fi

export FS_SERVER_HOME
export BASE_DIR=$(dirname $0)/..
export CLASSPATH=.:${CLASSPATH}:${BASE_DIR}/conf:${BASE_DIR}/lib/*

java -server -Dflyingsocks.config.location=${BASE_DIR}/conf -cp ${CLASSPATH} com.lzf.flyingsocks.server.ServerBoot $@

