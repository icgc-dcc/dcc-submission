#!/bin/bash
#
# usage: proxy$ ./dcc.sh ./params_file
#
# goal: prints max_open_files for important processes of the DCC submission system
#
# author: anthony.cros@oicr.on.ca
#
# assumptions:
# - running from . where ./dcc.sh exists
# - user running the script can ssh to the various hosts (ideally using a trusted connection) and from there can sudo to each special user (hdfs, mapred, mongodb)
#

# ===========================================================================

params_file=${1?}
script="./limit.sh"

# sanity checks
[ -f ${params_file?} ] || { echo "ERROR: cannot find ${params_file?}"; exit 1; }
[ -f ${script?} ] || { echo "ERROR: cannot find ${script?}"; exit 1; }

# export the hosts to explore
source ${params_file?}

# ===========================================================================

# for a given host/user/keyword, will print out the effective max_open_files of the corresponding PID
function process() {
 host=${1?} && shift
 user=${1?} && shift
 keyword=${1?} && shift
 ssh ${host?} 'bash -s' < ${script?} ${host?} ${user?} "${keyword?}"
}

# ===========================================================================
# explore each host and report on the effective max_open_files value

## www: hdfs     30367 23.6 25.0 2530496 2028996 pts/3 Sl   May30 258:30 java -Dlogback.configurationFile=/var/lib/hdfs/dcc/server/logback.xml -cp dcc-submission-server-1.6-SNAPSHOT.jar org.icgc.dcc.Main external /var/lib/hdfs/application.conf
process ${www?} "hdfs" "dcc.Main"

# ---------------------------------------------------------------------------

## nn: hdfs      1729  0.1  0.3 1328816 246636 ?      Sl   Apr24  83:36 /usr/lib/jvm/j2sdk1.6-oracle/jre/bin/java -Dproc_namenode -Xmx1000m -Dhdfs.audit.logger=INFO,RFAAUDIT -Dsecurity.audit.logger=INFO,RFAS -Djava.net.preferIPv4Stack=true -Dhadoop.log.dir=/var/log/hadoop-hdfs -Dhadoop.log.file=hadoop-cmf-hdfs1-NAMENODE-hcn50.res.oicr.on.ca.log.out -Dhadoop.home.dir=/usr/lib/hadoop -Dhadoop.id.str=hdfs -Dhadoop.root.logger=INFO,RFA -Djava.library.path=/usr/lib/hadoop/lib/native -Dhadoop.policy.file=hadoop-policy.xml -Djava.net.preferIPv4Stack=true -Xmx720732160 -Dhadoop.security.logger=INFO,RFAS org.apache.hadoop.hdfs.server.namenode.NameNode
process ${nn?} "hdfs" "namenode"

# ---------------------------------------------------------------------------

## nn2: ?
if [ -n "${nn2?}" ]; then
 process ${nn2?} "hdfs" "secondary" # FIXME: not sure about the keyword...
fi

# ---------------------------------------------------------------------------

## jt: mapred    1989  0.6  1.4 3346372 942132 ?      Sl   Apr22 372:05 /usr/lib/jvm/j2sdk1.6-oracle/jre/bin/java -Dproc_jobtracker -Xmx1000m -Djava.net.preferIPv4Stack=true -Xmx2755907842 -Dhadoop.event.appender=,EventCatcher -Dhadoop.log.dir=/var/log/hadoop-0.20-mapreduce -Dhadoop.log.file=hadoop-cmf-mapreduce1-JOBTRACKER-hcn51.res.oicr.on.ca.log.out -Dhadoop.home.dir=/usr/lib/hadoop-0.20-mapreduce -Dhadoop.id.str= -Dhadoop.root.logger=INFO,RFA -Djava.library.path=/usr/lib/hadoop-0.20-mapreduce/lib/native/Linux-amd64-64 -Dhadoop.policy.file=hadoop-policy.xml -classpath /var/run/cloudera-scm-agent/process/305-mapreduce-JOBTRACKER:/usr/lib/jvm/j2sdk1.6-oracle/jre/lib/tools.jar:/usr/lib/hadoop-0.20-mapreduce:/usr/lib/hadoop-0.20-mapreduce/hadoop-core-2.0.0-mr1-cdh4.1.2.jar:/usr/lib/hadoop-0.20-mapreduce/lib/activation-1.1.jar:/usr/lib/hadoop-0.20-mapreduce/lib/ant-contrib-1.0b3.jar:/usr/lib/hadoop-0.20-mapreduce/lib/asm-3.2.jar:/usr/lib/hadoop-0.20-mapreduce/lib/aspectjrt-1.6.5.jar:/usr/lib/hadoop-0.20-mapreduce/lib/aspectjtools-1.6.5.jar:/usr/lib/hadoop-0.20-mapreduce/lib/avro-1.7.1.cloudera.2.jar:/usr/lib/hadoop-0.20-mapreduce/lib/avro-compiler-1.7.1.cloudera.2.jar:/usr/lib/hadoop-0.20-mapreduce/lib/commons-beanutils-1.7.0.jar:/usr/lib/hadoop-0.20-mapreduce/lib/commons-beanutils-core-1.8.0.jar:/usr/lib/hadoop-0.20-mapreduce/lib/commons-cli-1.2.jar:/usr/lib/hadoop-0.20-mapreduce/lib/commons-codec-1.4.jar:/usr/lib/hadoop-0.20-mapreduce/lib/commons-collections-3.2.1.jar:/usr/lib/hadoop-0.20-mapreduce/lib/commons-configuration-1.6.jar:/usr/lib/hadoop-0.20-mapreduce/lib/commons-digester-1.8.jar:/usr/lib/hadoop-0.20-mapreduce/lib/commons-el-1.0.jar:/usr/lib/hadoop-0.20-mapreduce/lib/commons-httpclient-3.1.jar:/usr/lib/hadoop-0.20-mapreduce/lib/commons-io-2.1.jar:/usr/lib/hadoop-0.20-mapreduce/lib/commons-lang-2.5.jar:/usr/lib/hadoop-0.20-mapreduce/lib/commons-logging-1.1.1.jar:/usr/lib/hadoop-0.20-mapreduce/lib/commons-math-2.1.jar:/usr/lib/hadoop-0.20-mapreduce/lib/commons-net-3.1.jar:/usr/lib/hadoop-0.20-mapreduce/lib/guava-11.0.2.jar:/usr/lib/hadoop-0.20-mapreduce/lib/hadoop-fairscheduler-2.0.0-mr1-cdh4.1.2.jar:/usr/lib/hadoop-0.20-mapreduce/lib/hsqldb-1.8.0.10.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jackson-core-asl-1.8.8.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jackson-jaxrs-1.8.8.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jackson-mapper-asl-1.8.8.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jackson-xc-1.8.8.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jasper-compiler-5.5.23.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jasper-runtime-5.5.23.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jaxb-api-2.2.2.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jaxb-impl-2.2.3-1.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jersey-core-1.8.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jersey-json-1.8.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jersey-server-1.8.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jets3t-0.6.1.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jettison-1.1.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jetty-6.1.26.cloudera.2.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jetty-util-6.1.26.cloudera.2.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jsch-0.1.42.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jsp-api-2.1.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jsr305-1.3.9.jar:/usr/lib/hadoop-0.20-mapreduce/lib/junit-4.8.2.jar:/usr/lib/hadoop-0.20-mapreduce/lib/kfs-0.2.2.jar:/usr/lib/hadoop-0.20-mapreduce/lib/kfs-0.3.jar:/usr/lib/hadoop-0.20-mapreduce/lib/log4j-1.2.17.jar:/usr/lib/hadoop-0.20-mapreduce/lib/mockito-all-1.8.5.jar:/usr/lib/hadoop-0.20-mapreduce/lib/paranamer-2.3.jar:/usr/lib/hadoop-0.20-mapreduce/lib/protobuf-java-2.4.0a.jar:/usr/lib/hadoop-0.20-mapreduce/lib/servlet-api-2.5.jar:/usr/lib/hadoop-0.20-mapreduce/lib/slf4j-api-1.6.1.jar:/usr/lib/hadoop-0.20-mapreduce/lib/snappy-java-1.0.4.1.jar:/usr/lib/hadoop-0.20-mapreduce/lib/stax-api-1.0.1.jar:/usr/lib/hadoop-0.20-mapreduce/lib/xmlenc-0.52.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jsp-2.1/jsp-2.1.jar:/usr/lib/hadoop-0.20-mapreduce/lib/jsp-2.1/jsp-api-2.1.jar:/usr/lib/hadoop-0.20-mapreduce/contrib/capacity-scheduler/hadoop-capacity-scheduler-2.0.0-mr1-cdh4.1.2.jar:/usr/share/cmf/lib/plugins/event-publish-4.1.2-shaded.jar:/usr/lib/hado
process ${jt?} "mapred" "jobtracker"

# ---------------------------------------------------------------------------

## dn/tt:
for worker in ${workers?}; do
 # dn: hdfs      1680  0.5  1.7 2302768 1113320 ?     Sl   Mar22 601:08 /usr/lib/jvm/j2sdk1.6-oracle/jre/bin/java -Dproc_datanode -Xmx1000m -Dhdfs.audit.logger=INFO,RFAAUDIT -Dsecurity.audit.logger=INFO,RFAS -Djava.net.preferIPv4Stack=true -Dhadoop.log.dir=/var/log/hadoop-hdfs
 process ${worker?} "hdfs" "datanode"

 # tt: mapred    1875  0.5  0.6 2810332 421824 ?      Sl   Mar22 544:27 /usr/lib/jvm/j2sdk1.6-oracle/jre/bin/java -Dproc_tasktracker -Xmx1000m -Djava.net.preferIPv4Stack=true -Xmx864878592 -Dhadoop.event.appender=,EventCatcher -Dhadoop.log.dir=/var/log/hadoop-0.20-mapreduce -D
 process ${worker?} "mapred" "tasktracker"
done

# ---------------------------------------------------------------------------

## mongos: mongodb   2377 11.0 24.3 28695548 15829532 ?   Sl   Apr10 8099:33 /usr/bin/mongod --config /etc/mongodb.conf
for mongo in ${mongos?}; do
 process ${mongo?} "mongodb" "mongod"
done

# ===========================================================================

exit

