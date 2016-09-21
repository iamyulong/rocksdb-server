#!/bin/bash

VERSION=0.2

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# run as background process
echo "starting up server"
cd $DIR/../
java -cp "$DIR/rocksdb-server-${VERSION}.jar:$DIR/../lib/*" com.ranksays.rocksdb.server.Main &
