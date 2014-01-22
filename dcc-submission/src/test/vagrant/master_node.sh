#!/bin/bash -vx

# setup apt
export DEBIAN_FRONTEND=noninteractive

# setup Mongo
# get key
apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10
# get the aptget settings
echo 'deb http://downloads-distro.mongodb.org/repo/ubuntu-upstart dist 10gen' | tee /etc/apt/sources.list.d/mongodb.list
apt-get update
apt-get -q -y --force-yes install mongodb-10gen=2.4.1
service mongodb restart
# sleep for 5 minutes to ensure Mongo comes online
sleep 3m


