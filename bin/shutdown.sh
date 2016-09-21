#!/bin/bash

# find the pid
pid=`ps aux | grep "[r]ocksdb-server" | awk '{print $2}'`

if [ "$pid" != "" ];then
	echo "shutting down server (pid = $pid)"
	kill -15 $pid
fi
