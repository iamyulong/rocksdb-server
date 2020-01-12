#!/bin/sh

dir=$(dirname "$0")
cd "$dir/.."

# find the pid
pid=$(ps aux | grep "[r]ocksdb-server" | awk '{print $2}')

if [ "$pid" != "" ];then
	echo "Shutting down server (pid = $pid) .."
	kill -15 "$pid"
fi
