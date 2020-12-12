#!/usr/bin/env bash

SYSTEM_BIT=`getconf LONG_BIT`

install_jdk() {
  java -version && return 0

  JDK_VERSION='java-1.8.0-openjdk-devel.i686'

  if [ $SYSTEM_BIT == 64 ]; then
      JDK_VERSION='java-1.8.0-openjdk-devel.x86_64'
  fi

  yum install $JDK_VERSION && return 0
  apt-get install $JDK_VERSION && return 0
  return 1
}

install_maven() {
  mvn -version && return 0
  yum install maven && return 0
  apt-get install maven && return 0
  return 1
}


compile_project() {
  mvn clean package || return 1
  mvn assembly:single || return 1
  return 0
}


main() {
  return 0
}