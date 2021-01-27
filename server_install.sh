#!/usr/bin/env bash

# 安装目录
if [ -z "$FS_HOME" ]; then
  export FS_HOME=/opt/flyingsocks-server
fi

# 系统位数
SYSTEM_BIT=`getconf LONG_BIT`

# 系统运行内存，单位KB
SYSTEM_MEMORY=`awk '($1 == "MemTotal:"){print $2}' /proc/meminfo`

function install_jdk() {
  java -version && return 0

  JDK_VERSION='java-1.8.0-openjdk-devel.i686'

  if [[ $SYSTEM_BIT == 64 ]] && [[ $SYSTEM_MEMORY -ge 2097152 ]]; then
      JDK_VERSION='java-1.8.0-openjdk-devel.x86_64'
  fi

  yum install -y $JDK_VERSION && return 0
  apt-get install $JDK_VERSION && return 0
  return 1
}

function install_maven() {
  mvn -version && return 0
  yum install -y maven && return 0
  apt-get install maven && return 0
  return 1
}

# 安装必要的工具
function install_tools() {
  unzip -v && return 0
  yum install -y unzip openssl openssl-devel && return 0
  apt-get install unzip openssl openssl-devel && return 0
  return 1
}

function compile_project() {
  mvn clean install || return 1
  cd server || exit 1
  mvn assembly:single || return 1
  cd ..
  return 0
}

function main() {
  install_tools
  if [ $? -ne 0 ]; then
    echo "Install tools failure"
    exit 1
  fi

  install_jdk
  if [ $? -ne 0 ]; then
    echo "Install JDK failure"
    exit 1
  fi

  install_maven
  if [ $? -ne 0 ]; then
    echo "Install Maven failure"
    exit 1
  fi

  compile_project
  if [ $? -ne 0 ]; then
    echo "Compile project failure"
    exit 1
  fi

  unzip server/target/flyingsocks-server-bin.zip -d $FS_HOME/..
  echo "Server install success"
}

main