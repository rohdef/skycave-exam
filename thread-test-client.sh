#!/bin/bash

cd /root/cave
source setupLocalSocket.sh
export SKYCAVE_REACTOR_IMPLEMENTATION=cloud.cave.config.socket.ThreadedReactor
export SKYCAVE_CAVESTORAGE_IMPLEMENTATION=cloud.cave.server.service.ServerCaveStorage
export SKYCAVE_SUBSCRIPTION_IMPLEMENTATION=cloud.cave.manual.NullSubscriptionService
ant load.cave
