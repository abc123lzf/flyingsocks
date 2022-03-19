#!/usr/bin/env bash

if [ "$1" == "" ]; then
    echo "please input os version, optional versions: win64、win32、linux32 、linux64 and macos."
    exit 0
fi

# find FS_HOME
if [ -z "$FS_CLIENT_HOME" ] ; then
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
  FS_CLIENT_HOME=`dirname "$PRG"`/..
  # make it fully qualified
  FS_CLIENT_HOME=`cd "$FS_CLIENT_HOME" && pwd`
  cd "$saveddir"
fi

export FS_CLIENT_HOME
export JAVA="${JAVA_HOME}/bin/java"
export BASE_DIR=$(dirname $0)/..
export CLASSPATH=.:${CLASSPATH}:${BASE_DIR}/conf:${BASE_DIR}/lib/*

JAVA_OPT="${JAVA_OPT} -cp ${CLASSPATH}"
JAVA_OPT="${JAVA_OPT} -Dflyingsocks.config.location=${BASE_DIR}/conf"
if [ "$1" == "macos" ]; then
    JAVA_OPT="${JAVA_OPT} -XstartOnFirstThread"
fi

${JAVA} ${JAVA_OPT} com.lzf.flyingsocks.client.ClientBoot

