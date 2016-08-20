#!/bin/bash
# Copyright 2016(c) The Ontario Institute for Cancer Research. All rights reserved.
#
# DCC Submission - Docker entrypoint
#
# Descripton: 
#   Initializes the infrastructure after installation
# Usage: 
#   ./entrypoint.sh

echo '\n*** Starting Mongo...\n'
/usr/bin/mongod --fork --logpath /var/log/mongod.log

# Clear all logs
echo '\n*** Cleaning logs...\n'
rm -fr /var/log/hadoop-*/*

# Start HDFS
echo '\n*** Starting HDFS...\n'
service hadoop-hdfs-namenode start
service hadoop-hdfs-datanode start

echo '\n*** Initializing HDFS...\n'
su -l -c 'hadoop fs -mkdir -p hdfs:///tmp/hadoop-mapred/mapred/system' hdfs
su -l -c 'hadoop fs -chown -R mapred:hadoop hdfs:///tmp/hadoop-mapred' hdfs

# Hadoop 0.20 Job Tracker
echo '\n*** Starting M/R...\n'
service hadoop-0.20-mapreduce-tasktracker start
service hadoop-0.20-mapreduce-jobtracker start

echo '\n*** Finished!\n'
bash
	