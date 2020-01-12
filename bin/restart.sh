#!/bin/sh

dir=$(dirname "$0")
cd "$dir/.."

sh ./bin/shutdown.sh
sh ./bin/startup.sh
