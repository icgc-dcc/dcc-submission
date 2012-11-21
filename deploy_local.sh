#!/bin/bash -e
# see DCC-499
#
# example: ./deploy_local.sh my.dcc.server dev [true]
#
# notes:
# - this script is based on former https://wiki.oicr.on.ca/display/DCCSOFT/Standard+operating+procedures#Standardoperatingprocedures-SOPforDeployingtheserver (which also links to this script now)
# - assumptions:
#   - using Linux or Darwin
#   - must be in data-submission
#   - must have checked out wanted branch
#   - must have run "npm install" in ./client
#   - tests must run (else jar creation will fail)
#   - on the remote server, /var/lib/hdfs/log and /var/lib/hdfs/realm.ini already exist (the latter is the reference file)
# - convention: server expects client files under ../client (content should be that of ./client/public after build with brunch). this script takes care of building the appropriate directoy structure.

# ===========================================================================

dev_dir="."
dev_server_dir="${dev_dir?}/server"
dev_target_dir="${dev_server_dir?}/target"
dev_client_dir="${dev_dir?}/client"
dev_public_dir="${dev_client_dir?}/public"

dev_server_deploy_script_name="remote_startup.sh"
dev_server_deploy_script="${dev_dir?}/${dev_server_deploy_script_name?}"
parent_pom_file="${dev_dir?}/pom.xml"
server_pom_file="${dev_server_dir?}/pom.xml"

# ===========================================================================
# basic checks

# check mvn
hash mvn 2>&- || { echo "ERROR: mvn is not available"; exit 1; }

# check xpath
hash xpath 2>&- || { echo "ERROR: xpath is not available"; exit 1; }

function is_linux() {
 [ "$(uname -s)" == "Linux" ]
}
function is_mac() {
 [ "$(uname -s)" == "Darwin" ]
}
function get_xml_value() {
 file=${1?}
 path=${2?}
 if is_linux; then
  xpath -e "${path?}" "${file?}" 2>&-
 else
  xpath "${file?}" "${path?}" 2>&-
 fi
}

is_linux || is_mac || { echo "ERROR: must be using Linux or Mac (Darwin)"; exit 1; }

# check in data-submission directory
[ -f "${parent_pom_file?}" ] && [ "$(get_xml_value ${parent_pom_file?} '//project/artifactId/text()')" == "dcc-parent" ] || { echo "ERROR: must be in data-submission dir"; exit 1; }


# ===========================================================================

server=${1?}
mode=${2?}
skip_mvn=$3 && skip_mvn=${skip_mvn:="true"} && [ "${skip_mvn}" == "true" ] && skip_mvn=true || skip_mvn=false

echo "server=\"${server?}\""
echo "mode=\"${mode?}\""
echo "skip_mvn=\"${skip_mvn?}\""
valid_modes="local dev qa" && [ -n "$(echo "${valid_modes?}" | tr " " "\n" | awk '$0=="'"${mode?}"'"')" ] || { echo "ERROR: invalid mode: \"${mode?}\""; exit 1; }
if [ "${mode?}" == "local" ]; then
 mode=""
fi

timestamp=$(date "+%y%m%d%H%M%S")
echo "timestamp=\"${timestamp?}\""

artifact_id=$(get_xml_value ${server_pom_file?} '//project/artifactId/text()')
echo "artifact_id=\"${artifact_id?}\""
version=$(get_xml_value ${server_pom_file?} '//project/parent/version/text()')
echo "version=\"${version?}\""
jar_file_name="${artifact_id?}-${version?}.jar"
jar_file="${dev_target_dir?}/${jar_file_name?}"
echo "jar_file=\"${jar_file?}\""

# ===========================================================================

local_working_dir_name="dist_${timestamp?}"
local_working_dir="./${local_working_dir_name?}" # careful about changing that (rm -rf further down)
echo "local_working_dir=\"${local_working_dir?}\""

local_server_dir="${local_working_dir?}/server"
local_client_dir="${local_working_dir?}/client"

# ===========================================================================

# "build" project (mostly manual for now)
mkdir -p ${local_working_dir?} ${local_server_dir?} ${local_working_dir?}/log && { rm -rf "${local_client_dir?}" 2>&- || : ; }
if ! ${skip_mvn?}; then
 { cd ${dev_server_dir?} && mvn assembly:assembly -Dmaven.test.skip=true && cd .. ; } || { echo "ERROR: failed to build project (server)"; exit 1; } # critical
else
 ls ${jar_file?} >/dev/null || { echo "ERROR: ${jar_file?} does not exist"; exit 1; } # critical
 echo -n "skipping jar creation, re-using: ${jar_file?}"
 if is_linux; then
  latest=$(stat -c '%Y' ${jar_file?}) && latest=$(date --date="@${latest?}")
 else
  latest=$(date -r $(stat -f "%m" ${jar_file?}))
 fi
 echo " from \"${latest?}\""
fi
echo "building client files..."
{ cd "${dev_client_dir?}" && brunch build && cd .. ; } || { echo "ERROR: failed to build project (client)"; exit 1; } # critical

cp "${jar_file?}" "${local_server_dir?}/"
cp -r "${dev_public_dir?}" "${local_client_dir?}"
cp "${dev_server_deploy_script?}" "${local_working_dir?}/"

echo -e "content:\n"
find ${local_working_dir?}
echo

# ===========================================================================
# copy files to server

read -p "copy to server - please enter OICR username [default \"$USER\"]: " username
echo "username=\"${username?}\""
username=${username:=$USER}

remote_tmp_dir="/tmp/${local_working_dir_name?}"
echo "remote_tmp_dir=\"${remote_tmp_dir?}\""

echo "scp -r ${local_working_dir?} ${username?}@${server?}:${remote_tmp_dir?}"
echo "scp ${local_working_dir?} to server"
scp -r ${local_working_dir?} ${username?}@${server?}:${remote_tmp_dir?} # must use /tmp for now (permission problems)
#rm -rf ${local_working_dir?} && echo "${local_working_dir?} deleted" # remove working directory

# ===========================================================================
# start server remotely

hdfs_dir="/var/lib/hdfs"
backup_dir="${hdfs_dir?}/backup" # must be absolute path
remote_dir="${hdfs_dir?}/dcc" # must be absolute path
remote_realm_file="${hdfs_dir?}/realm.ini" # must exist already
remote_log_dir="${hdfs_dir?}/log" # must exist already
remote_server_dir="${remote_dir?}/server"
remote_client_dir="${remote_dir?}/client"
echo "remote_server_dir=\"${remote_server_dir?}\""
echo "remote_client_dir=\"${remote_client_dir?}\""
echo "remote_log_dir=\"${remote_log_dir?}\""

log_base="${artifact_id?}-${timestamp?}"
log_file="${remote_log_dir?}/${log_base?}.log"

if false; then # WIP
 echo && read -p "start server? [press Enter]"
 server_command="./${dev_server_deploy_script_name?} \"${timestamp?}\" \"${jar_file_name?}\" \"${log_file?}\" \"${remote_tmp_dir?}\" \"${remote_dir?}\" \"${remote_server_dir?}\" \"${remote_realm_file?}\""
 echo "server_command=\"${server_command?}\""
 sudo_command="sudo -u hdfs -i \"${server_command?}\""
 echo "sudo_command=\"${sudo_command?}\""
 screen_command="screen -S \"${sudo_command?}\""
 echo "screen_command=\"${screen_command?}\""
 echo "enter password to ssh and start server at ${server?}" && ssh ${username?}@${server?} "${sudo_command?}"
else
 echo "==========================================================================="
 echo "please issue the following commands on ${server?} (must become hdfs then use screen first - WIP):"
 echo
 echo "ssh ${username?}@${server?}"
 echo "sudo -u hdfs -i"
 echo
 echo "mv ${remote_dir?} ${backup_dir?}/dcc.${timestamp?}.bak"
 echo "cp -r ${remote_tmp_dir?} ${remote_dir?}"
 echo "cp ${remote_realm_file?} ${remote_server_dir?}/"
 echo "cd ${remote_server_dir?}"
 echo "# tail -f ${log_file?} # to be used elsewhere"
 echo
 echo "java -cp ${jar_file_name?} org.icgc.dcc.Main ${mode?} >> ${log_file?} 2>&1 # in screen session"
 echo
fi

# ===========================================================================

exit

