#!/bin/bash
# Copyright 2016(c) The Ontario Institute for Cancer Research. All rights reserved.
#
# DCC Submission - Docker entrypoint
#
# Descripton: 
#   Initializes the infrastructure after installation
# Usage: 
#   ./entrypoint.sh

echo
echo '*** Starting Mongo...'
echo

/usr/bin/mongod --fork --logpath /var/log/mongod.log

echo
echo '*** Cleaning logs...'
echo

rm -fr /var/log/hadoop-*/*

echo
echo '*** Starting HDFS...'
echo

service hadoop-hdfs-namenode start
service hadoop-hdfs-datanode start

echo
echo '*** Initializing HDFS...'
echo

su -l -c 'hadoop fs -mkdir -p hdfs:///tmp/hadoop-mapred/mapred/system' hdfs
su -l -c 'hadoop fs -chown -R mapred:hadoop hdfs:///tmp/hadoop-mapred' hdfs

echo
echo '*** Starting M/R...'
echo

service hadoop-0.20-mapreduce-tasktracker start
service hadoop-0.20-mapreduce-jobtracker start

echo
echo '*** Finished!'
echo

echo '...entering bash shell'
bash
	