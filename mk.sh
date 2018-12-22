#!/bin/bash

# Script is used to compile the Java sources (e.g. on raspberry pi)
# Run with ". mk.sh"
# Author: fmangels

mkdir -p bin/

jars=$(echo $PWD/lib/* | tr " " ":")
CLASSPATH=".:$jars:$PWD/bin"

export CLASSPATH
echo "export CLASSPATH=$CLASSPATH"

find src/ -iname *.java > build_sources

#/usr/lib/jvm/java-8-openjdk-amd64/bin/javac -d bin/ @build_sources -verbose
javac -d bin/ @build_sources -verbose
