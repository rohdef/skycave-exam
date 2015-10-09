#!/bin/bash

source setupLocalSocket.sh
export SKYCAVE_APPSERVER=cave.smatso.dk:37124
rlwrap ant cmd 
