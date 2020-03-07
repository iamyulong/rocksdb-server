# RocksDB Server

This is a lightweight RocksDB Server based on jetty.

## Features

* Multiple concurrent databases;
* Simple API interface and client libraries;
* Basic Authentication;
* Batch `put`, `get` and `remove` operations.

## Prerequisites

You need to have a Java 8 or above runtime installed.

## Get started

1. Download and unzip the latest binary release from [here](https://github.com/iamyulong/rocksdb-server/releases);
2. Update the configuration file at `./conf/server.json`;
3. Start up the server: `./bin/startup.sh`.

Once boot up, you can check if it's working via http://localhost:8516.

## Install as system service (Ubuntu)

1. Copy the following config to `/etc/init.d/rocksdb-server`, after replacing `SERVER_ROOT` with a real path:

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

2. Update the permissions and enable service:

    ```bash
    chmod 755 /etc/init.d/rocksdb-server
    update-rc.d rocksdb-server defaults
    ```

3. Now, you can start/stop/restart your server via the following commands:

    ```bash
    service rocksdb-server start
    service rocksdb-server stop
    service rocksdb-server restart
    ```


## Build from source

```bash
git clone https://github.com/iamyulong/rocksdb-server
cd rocksdb-server
mvn clean install
```