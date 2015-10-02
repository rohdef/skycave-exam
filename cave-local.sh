#!/bin/bash

ant clean
source setupLocalSocket.sh
rlwrap ant cmd
