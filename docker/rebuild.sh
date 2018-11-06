#!/bin/sh

set -e
set -x

cd .. && ./gradlew assemble && cd docker

mkdir -p elasticsearch/plugins
cp  ../build/distributions/*.zip elasticsearch/plugins

docker-compose build

rm -rf elasticsearch/plugins/*
