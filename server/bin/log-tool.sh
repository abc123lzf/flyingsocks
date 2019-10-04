#!/usr/bin/env bash

params=""

for i in $* ; do
    params="$params $i"
done

cmd="java -Xbootclasspath/a:../conf:../ -jar ../lib/flyingsocks-server-2.0.jar com.lzf.flyingsocks.server.tools.LogTool $params"

${cmd}