#!/bin/bash

cd /root/cave
source setupLocalMQTopic.sh
export SKYCAVE_APPSERVER=mq0:5672
export SKYCAVE_DBSERVER=db0:27017
ant ocean-daemon -Dregion=$1
