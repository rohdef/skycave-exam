#!/bin/bash

cd /root/cave
ant clean
source setupLocalMQ.sh
ant ocean-daemon
