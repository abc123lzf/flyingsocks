#!/usr/bin/env bash

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

  yum install $JDK_VERSION && return 0
  apt-get install $JDK_VERSION && return 0
  return 1
}

function install_maven() {
  mvn -version && return 0
  yum install maven && return 0
  apt-get install maven && return 0
  return 1
}

# 安装必要的工具
function install_tools() {
  unzip -v && return 0
  yum install unzip && return 0
  apt-get install unzip && return 0
  return 1
}

function compile_project() {
  mvn clean package || return 1
  mvn assembly:single || return 1
  return 0
}

function main() {
  install_tools
  if [ $? == 0 ]; then
    echo "Install tools failure"
    exit 1
  fi

  install_jdk
  if [ $? == 0 ]; then
    echo "Install JDK failure"
    exit 1
  fi

  install_maven
  if [ $? == 0 ]; then
    echo "Install Maven failure"
    exit 1
  fi

  compile_project
  if [ $? == 0 ]; then
    echo "Compile project failure"
    exit 1
  fi

  cp server/target/flyingsocks-server-bin.zip ..
  unzip ../flyingsocks-server-bin.zip

  echo "Server install success"
}

main