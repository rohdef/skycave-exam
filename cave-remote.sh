#!/bin/bash

ant clean
source setupLocalMQ.sh
export SKYCAVE_APPSERVER=cave.smatso.dk:5672
rlwrap ant cmd
