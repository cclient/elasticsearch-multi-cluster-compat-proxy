#!/bin/sh
mvn clean package -Dmaven.test.skip=true
docker build -t cclient/elasticsearch-multi-cluster-compat-proxy:$1 ./
docker push cclient/elasticsearch-multi-cluster-compat-proxy:$1
