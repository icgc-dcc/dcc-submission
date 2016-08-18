#!/bin/bash

/usr/bin/mongod --fork --logpath /var/log/mongod.log

service hadoop-hdfs-namenode start
service hadoop-hdfs-datanode start

sudo -u hdfs hadoop fs -chmod 777 /
sudo -u hdfs hadoop fs -mkdir -p /user/hdfs
sudo -u hdfs hadoop fs -chown hdfs /user/hdfs
sudo -u hdfs hadoop fs -mkdir /tmp
sudo -u hdfs hadoop fs -chmod -R 1777 /tmp
sudo -u hdfs hadoop fs -mkdir -p /var/lib/hadoop-hdfs/cache/mapred/mapred/staging
sudo -u hdfs hadoop fs -chmod 1777 /var/lib/hadoop-hdfs/cache/mapred/mapred/staging
sudo -u hdfs hadoop fs -mkdir -p /var/lib/hadoop-hdfs/cache/mapred/mapred/system
sudo -u hdfs hadoop fs -chmod 1777 /var/lib/hadoop-hdfs/cache/mapred/mapred/system
sudo -u hdfs hadoop fs -chown -R mapred /var/lib/hadoop-hdfs/cache/mapred

service hadoop-0.20-mapreduce-tasktracker start
service hadoop-0.20-mapreduce-jobtracker start

sleep 1

# tail log directory
tail -n 1000 -f /var/log/hadoop-*/*.out
	