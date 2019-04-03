#!/bin/sh

version=`git rev-parse HEAD`
name=rocksdb-server-`echo $version | cut -c1-8`

echo "Building: ${name}"
mvn install || exit 1

dir=dist/${name}
mkdir -p ${dir} || exit 2
cp -r lib ${dir} || exit 3
cp -r bin ${dir} || exit 3
cp -r conf ${dir} || exit 3

(cd dist; tar -cvzf ${name}.tar.gz ${name})