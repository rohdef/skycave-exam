#!/bin/bash

cd /root/cave
ant clean
source setupLocalMQ.sh
export SKYCAVE_APPSERVER=mq0:5672
ant ocean-daemon
