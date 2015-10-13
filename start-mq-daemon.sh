#!/bin/bash

cd /root/cave
source setupLocalMQ.sh
export SKYCAVE_APPSERVER=localhost:5672
export SKYCAVE_DBSERVER=db0:27017,db1:27017,db2:27017
ant ocean-daemon
