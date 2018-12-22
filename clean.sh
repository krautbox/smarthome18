#!/bin/bash

# Cleans up the software directory / build resources
# Author: fmangels

rm -rf bin
rm -f build_sources
rm -f safer_smart_home.log

# remove old swap files
# find . -name '.*.sw?' | xargs rm
