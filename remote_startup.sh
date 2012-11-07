#!/bin/bash -e
# see DCC-499
#
# usage: do not use directly! it is used by ./deploy_local.sh

echo "WIP: do not use for now" && exit 1

# ===========================================================================
# basic checks

# check java
hash java 2>&- || { echo "ERROR: java is not available"; exit 1; }

# ===========================================================================

timestamp="${1?}" && shift
jar_file_name="${1?}" && shift
log_file="${1?}" && shift
remote_tmp_dir="${1?}" && shift
remote_dir="${1?}" && shift
remote_server_dir="${1?}" && shift
remote_realm_file="${1?}"

echo "timestamp=\"${timestamp?}\""
echo "jar_file_name=\"${jar_file_name?}\""
echo "log_file=\"${log_file?}\""
echo "remote_tmp_dir=\"${remote_tmp_dir?}\""
echo "remote_dir=\"${remote_dir?}\""
echo "remote_server_dir=\"${remote_server_dir?}\""
echo "remote_realm_file=\"${remote_realm_file?}\""

# WIP (must nohup/screen), for now use screen (as hdfs) and roughly:

mv ${remote_dir?} ${backup_dir?}/dcc.${timestamp?}.bak
cp -r ${remote_tmp_dir?} ${remote_dir?}
cp ${remote_realm_file?} ${remote_server_dir?}/
cd ${remote_server_dir?}
echo "tail -f ${log_file?}"
java -cp ${jar_file_name?} org.icgc.dcc.Main ${mode?} >> ${log_file?} 2>&1


