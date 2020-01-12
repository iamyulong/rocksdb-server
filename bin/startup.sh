#!/bin/sh

dir=$(dirname "$0")
cd "$dir/.."

echo "Starting up server .."
java -cp "./lib/*" com.ranksays.rocksdb.server.Main &
