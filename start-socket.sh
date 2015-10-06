#!/bin/bash

cd /root/cave
ant clean
source setupLocalSocket.sh
export SKYCAVE_CAVESTORAGE_IMPLEMENTATION=cloud.cave.doubles.FakeCaveStorage
ant ocean-daemon
