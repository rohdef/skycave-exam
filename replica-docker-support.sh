#!/bin/bash

docker stop db{0,1,2}
docker rm db{0,1,2}

docker run --name db0 --hostname db0 -d mongo --smallfiles --noprealloc --replSet "rs0"
docker run --name db1 --hostname db1 -d mongo --smallfiles --noprealloc --replSet "rs0"
docker run --name db2 --hostname db2 -d mongo --smallfiles --noprealloc --replSet "rs0"

docker inspect db{0,1,2} | grep \"IPAdd
