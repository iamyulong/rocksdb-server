# rocksdb-server
A tiny HTTP-based server for RocksDB

## Features

* Use RocksDB as key-value storage engine
* HTTP-base API interface
* Authorization
* Support basic operations: get, put, remove, drop_database
* Batch get/put/remove (not atomic)

## Prerequisites
You need to have Java 8+ installed. Currently supported platform includes Mac OS X and Linux.

## How to use

1. Download the latest release from <https://github.com/iamyulong/rocksdb-server/releases>.
2. Unarchive and modify the configuration file at `conf/server.json`.
3. Start the server via terminal: `./bin/startup.sh`.
4. You can verify if it is working by visiting: <http://localhost:8516>

## Build from source

```bash
git clone https://github.com/iamyulong/rocksdb-server
cd rocksdb-server
mvn package
```

## Install as system service (Linux)

Edit SERVER_ROOT in the following script and copy to `/etc/init.d/rocksdb-server`.
```bash
#!/bin/bash

### BEGIN INIT INFO
# Provides:        rocksdb-server
# Required-Start:  $network
# Required-Stop:   $network
# Default-Start:   2 3 4 5
# Default-Stop:    0 1 6
# Short-Description: start/stop rocksdb-server
### END INIT INFO

SERVER_ROOT=/path/to/rocksdb/server

case $1 in
    start)
        /bin/bash $SERVER_ROOT/bin/startup.sh
    ;;
    stop)
        /bin/bash $SERVER_ROOT/bin/shutdown.sh
    ;;
    restart)
        /bin/bash $SERVER_ROOT/bin/restart.sh
    ;;
esac
exit 0
```
Change its permissions and add the correct symlinks automatically.
```bash
chmod 755 /etc/init.d/rocksdb-server
update-rc.d rocksdb-server defaults
```
From now on, you can start/stop/restart your server via the following commands:
```bash
service rocksdb-server start
service rocksdb-server stop
service rocksdb-server restart
```
