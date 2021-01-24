cmake_minimum_required(VERSION 3.17)
project(flyingsocks_native C)

set(CMAKE_C_STANDARD 99)

include_directories($ENV{JAVA_HOME}/include)
include_directories($ENV{JAVA_HOME}/include/linux)
add_library(flyingsocks_transparent SHARED com_lzf_flyingsocks_client_proxy_transparent_LinuxNative.h transparent.c transparent.h)