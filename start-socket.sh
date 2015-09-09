#!/bin/bash

cd /root/cave
ant clean
source setupLocalSocket.sh
ant ocean-daemon
