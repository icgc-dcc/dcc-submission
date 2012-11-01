#!/bin/bash -e
# see DCC-499
#
# usage: use output from ./deploy_local.sh (will echo exact command)
#
# notes:
# - this script is based on former https://wiki.oicr.on.ca/display/DCCSOFT/Standard+operating+procedures#Standardoperatingprocedures-SOPforDeployingtheserver (which also links to this script now)

echo "WIP: do not use for now" && exit 1

# ===========================================================================
# basic checks

# check java
hash java 2>&- || { echo "ERROR: java is not available"; exit 1; }

# ===========================================================================

server=${1?}
timestamp="${?}"
jar_file_name="${?}"
log_file="${?}"
hdfs_dir="${?}"
remote_working_dir="${?}"
remote_dir="${?}"
remote_server_dir="${?}"
remote_realm_file="${?}"

echo "server=\"${server?}\""
echo "timestamp=\"${timestamp?}\""
echo "jar_file_name=\"${jar_file_name?}\""
echo "log_file=\"${log_file?}\""
echo "hdfs_dir=\"${hdfs_dir?}\""
echo "remote_working_dir=\"${remote_working_dir?}\""
echo "remote_dir=\"${remote_dir?}\""
echo "remote_server_dir=\"${remote_server_dir?}\""
echo "remote_realm_file=\"${remote_realm_file?}\""

#TODO: must screenify this" #TODO: must screenify this: screen -S "dcc-server_${timestamp?}" - WIP
#cd ./dcc/server && java -cp ${jar_file?} org.icgc.dcc.Main prod >${log_file?} 2>&1 &

# WIP (must nohup/screen), for now use screen (as hdfs) and roughly:"
mv ${remote_dir?} ${hdfs_dir?}/backup/dcc.${timestamp?}.bak
cp -r ${remote_working_dir?} ${remote_dir?}
cp ${remote_realm_file?} ${remote_server_dir?}/
cd ${remote_server_dir?}
echo "tail -f ${log_file?}"
java -cp ${jar_file_name?} org.icgc.dcc.Main prod > ${log_file?} 2>&1


