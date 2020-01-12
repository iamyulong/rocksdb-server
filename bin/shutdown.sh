#!/bin/sh

dir=$(dirname "$0")
cd "$dir/.."

# find the pid
pid=$(pgrep -f "com.ranksays.rocksdb.server.Main")

if [ "$pid" != "" ];then
	echo "Shutting down server (pid = $pid) .."
	kill -15 "$pid"
fi
