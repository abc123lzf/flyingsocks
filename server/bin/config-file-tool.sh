#!/usr/bin/env bash

params=""

for i in $* ; do
    params="$params $i"
done

cmd="java -Xbootclasspath/a:../conf:../ -jar ../lib/flyingsocks-server-1.1.jar com.lzf.flyingsocks.server.tools.ConfigFileTool $params"

${cmd}