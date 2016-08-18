#     ______________________   _____       __              _           _           
#    /  _/ ____/ ____/ ____/  / ___/__  __/ /_  ____ ___  (_)_________(_)___  ____ 
#    / // /   / / __/ /       \__ \/ / / / __ \/ __ `__ \/ / ___/ ___/ / __ \/ __ \
#  _/ // /___/ /_/ / /___    ___/ / /_/ / /_/ / / / / / / (__  |__  ) / /_/ / / / /
# /___/\____/\____/\____/   /____/\__,_/_.___/_/ /_/ /_/_/____/____/_/\____/_/ /_/ 
                                                                                 
# Banner @ http://goo.gl/fYl2Pr

FROM nimmis/java:oracle-8-jdk
MAINTAINER ICGC <dcc-support@icgc.org>

# Base image doesn't start in root
WORKDIR /

#
# Install MongoDB
#

RUN \
  apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 7F0CEB10 && \
  echo 'deb http://downloads-distro.mongodb.org/repo/ubuntu-upstart dist 10gen' | sudo tee /etc/apt/sources.list.d/mongodb.list && \
  apt-get update && \
  apt-get install -y mongodb-10gen=2.4.4
  
RUN mkdir -p /data/db /data/configdb \
	&& chown -R mongodb:mongodb /data/db /data/configdb

#
# Install Hadoop
#

# Add the CDH 5 repository
COPY conf/cloudera.list /etc/apt/sources.list.d/cloudera.list

# Set preference for cloudera packages
COPY conf/cloudera.pref /etc/apt/preferences.d/cloudera.pref

# Add a Repository Key
RUN wget http://archive.cloudera.com/cdh5/ubuntu/trusty/amd64/cdh/archive.key -O archive.key && sudo apt-key add archive.key && \
    sudo apt-get update

# Install CDH package and dependencies
RUN sudo apt-get install -y hadoop-0.20-conf-pseudo

# Copy updated config files
COPY conf/core-site.xml /etc/hadoop/conf/core-site.xml
COPY conf/hdfs-site.xml /etc/hadoop/conf/hdfs-site.xml
COPY conf/hadoop-env.sh /etc/hadoop/conf/hadoop-env.sh
COPY conf/fair-scheduler.xml /etc/hadoop/conf/fair-scheduler.xml

# Format HDFS
RUN sudo -u hdfs hdfs namenode -format

#
# Setup
#

# Execution script
COPY conf/run.sh /usr/bin/run.sh
RUN chmod +x /usr/bin/run.sh

# Mongo
EXPOSE 27017 28017

# NameNode (HDFS)
EXPOSE 8020 50070

# DataNode (HDFS)
EXPOSE 50010 50020 50075

# JobTracker (M/R)
EXPOSE 50030 50060

#
# Entrypoint
#

CMD ["/usr/bin/run.sh"]
