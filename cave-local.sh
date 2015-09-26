#!/bin/bash

ant clean
source setupLocalMQ.sh
rlwrap ant cmd
