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

# Avoid ERROR: invoke-rc.d: policy-rc.d denied execution of start.
RUN echo "#!/bin/sh\nexit 0" > /usr/sbin/policy-rc.d

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
COPY conf/mapred-site.xml /etc/hadoop/conf/mapred-site.xml
COPY conf/hadoop-env.sh /etc/hadoop/conf/hadoop-env.sh

# Format the namenode if it's not already done
RUN su -l -c 'mkdir -p /var/lib/hadoop-hdfs/hdfs-data/namenode /var/lib/hadoop-hdfs/hdfs-data/datanode && hdfs namenode -format -nonInteractive' hdfs

# Setup other Hadoop directories
RUN su -l -c 'mkdir -p /var/lib/hadoop-hdfs/dn_socket' hdfs

#
# Setup
#

# Execution script
COPY bin/entrypoint.sh /usr/bin/entrypoint.sh
RUN chmod +x /usr/bin/entrypoint.sh

# Mongo
EXPOSE 27017 28017

# NameNode (HDFS)
EXPOSE 8020 50070

# DataNode (HDFS)
EXPOSE 50010 50020 50075

# JobTracker (M/R)
EXPOSE 8021 50030 50060

#
# Entrypoint
#

# docker run -it -p 27017:27017 -p 8020:8020 -p 8021:8021 -p 50070:50070 -p 50010:50010 -p 50020:50020 -p 50075:50075 -p 50030:50030 -p 50060:50060 dcc-submission
CMD ["/usr/bin/entrypoint.sh"]
