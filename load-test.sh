#!/bin/bash

cd /root/cave
source setupLocalSocket.sh
export SKYCAVE_CAVESTORAGE_IMPLEMENTATION=cloud.cave.server.service.ServerCaveStorage
ant load.mongo
