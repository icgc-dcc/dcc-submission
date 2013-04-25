#!/bin/bash -e
# see DCC-499
#
# usage: ./deploy_local.sh my.dcc.server my_mode [optional flags...]
# example:
#  src/main/scripts/deploy_local.sh ***REMOVED*** dev # deploy on dev
#  src/main/scripts/deploy_local.sh ***REMOVED*** dev false false # deploy on dev, don't skip mvn tests
#  src/main/scripts/deploy_local.sh ***REMOVED*** dev false ignoreme true # deploy on dev, skip jar generation altogther (assumes there's an existing jar from a previous run)
#
# notes:
# - this script is based on former https://wiki.oicr.on.ca/display/DCCSOFT/Standard+operating+procedures#Standardoperatingprocedures-SOPforDeployingtheserver (which also links to this script now)
# - assumptions:
#   - using Linux or Darwin
#   - must be in "dcc" directory (or wherever the utmost parent pom is)
#   - must have checked out wanted branch
#   - must have run "npm install" in ./dcc-submission-ui
#   - tests must run (else jar creation will fail)
#   - on the remote server, /var/lib/hdfs/log and /var/lib/hdfs/realm.ini already exist (the latter is the reference file)
# - convention: server on the deployment machine expects ui files under ../client (as a sibling basically). Content should be that of ./dcc-submission/dcc-submission-ui/public after build with brunch). this script takes care of building the appropriate directoy structure.

# ===========================================================================

dev_dir="."
original_pwd=$PWD
dcc_submission_dir="${dev_dir?}/dcc-submission"

dev_server_dir="${dcc_submission_dir?}/dcc-submission-server"
dev_target_dir="${dev_server_dir?}/target"
dev_client_dir="${dcc_submission_dir?}/dcc-submission-ui"
dev_public_dir="${dev_client_dir?}/public"

parent_pom_file="${dev_dir?}/pom.xml"
server_pom_file="${dev_server_dir?}/pom.xml"

main_class="org.icgc.dcc.Main"

parent_dir="dcc"

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

# Check one is in ${parent_dir?} directory
[ -f "${parent_pom_file?}" ] && [ "$(get_xml_value ${parent_pom_file?} '//project/artifactId/text()')" == "${parent_dir?}" ] || { echo "ERROR: must be in ${parent_dir?} dir"; exit 1; }


# ===========================================================================

server=${1?}
mode=${2?}
skip_scp=$3 && skip_scp=${skip_scp:="false"} && [ "${skip_scp}" == "true" ] && skip_scp=true || skip_scp=false
skip_test=$4 && skip_test=${skip_test:="true"} && [ "${skip_test}" == "true" ] && skip_test=true || skip_test=false
skip_mvn=$5 && skip_mvn=${skip_mvn:="false"} && [ "${skip_mvn}" == "true" ] && skip_mvn=true || skip_mvn=false # not recommended

echo "server=\"${server?}\""
echo "mode=\"${mode?}\""
echo "skip_mvn=\"${skip_mvn?}\""
valid_modes="local dev" && [ -n "$(echo "${valid_modes?}" | tr " " "\n" | awk '$0=="'"${mode?}"'"')" ] || { echo "ERROR: invalid mode: \"${mode?}\""; exit 1; }
if [ "${mode?}" == "local" ]; then
 mode=""
fi

timestamp="$(date '+%Y%m%d')T$(date '+%H%M%S')"
echo "timestamp=\"${timestamp?}\""

artifact_id=$(get_xml_value ${server_pom_file?} '//project/artifactId/text()')
echo "artifact_id=\"${artifact_id?}\""
version=$(get_xml_value ${server_pom_file?} '//project/parent/version/text()')
echo "version=\"${version?}\""
jar_file_name="${artifact_id?}-${version?}.jar"
jar_file="${dev_target_dir?}/${jar_file_name?}"
echo "jar_file=\"${jar_file?}\""
git_hash=$(git rev-parse --short HEAD)
echo "git_hash=\"${git_hash?}\""

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
 { cd ${dev_server_dir?} && mvn assembly:assembly -Dmaven.test.skip=${skip_test?} && cd ${original_pwd?} ; } || { echo "ERROR: failed to build project (server)"; exit 1; } # critical
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

{ cd "${dev_client_dir?}" && brunch b -m && cd ${original_pwd?} ; } || { echo "ERROR: failed to build project (client)"; exit 1; } # critical

cp "${jar_file?}" "${local_server_dir?}/"
cp -r "${dev_public_dir?}" "${local_client_dir?}"
echo "${git_hash?}" > "${local_client_dir?}/git.md5" # DCC-555

echo -e "content:\n"
find ${local_working_dir?}
echo

# ===========================================================================
# copy files to server

remote_tmp_dir="/tmp/${local_working_dir_name?}"
echo "remote_tmp_dir=\"${remote_tmp_dir?}\""

if ${skip_scp?}; then
 echo "skipping actual scp to ${server?}"
else
 read -p "copy to server - please enter OICR username [default \"$USER\"]: " username
 username=${username:=$USER}
 echo "username=\"${username?}\""

 echo "scp -r ${local_working_dir?} ${username?}@${server?}:${remote_tmp_dir?}"
 echo "scp ${local_working_dir?} to server"
 scp -r ${local_working_dir?} ${username?}@${server?}:${remote_tmp_dir?} # must use /tmp for now (permission problems)
 #rm -rf ${local_working_dir?} && echo "${local_working_dir?} deleted" # remove working directory
fi

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

echo "==========================================================================="
echo "please issue the following commands on ${server?}:"
echo
if ${skip_scp?}; then
 echo "# copy ${local_working_dir?} to ${server?}:${remote_tmp_dir?}"
 echo
fi
echo "ssh ${username:='your_user'}@${server?}"
echo "sudo -u hdfs -i"
echo
echo "current_pid=\$(jps -lm | grep \"${main_class?} ${mode?}\" | awk '{print "'$1'"}') && read -p \"kill \$current_pid?\" && kill \$current_pid"
echo "mv ${remote_dir?} ${backup_dir?}/dcc.${timestamp?}.bak"
echo "cp -r ${remote_tmp_dir?} ${remote_dir?}"
echo "cp ${remote_realm_file?} ${remote_server_dir?}/"
echo "cd ${remote_server_dir?}"
echo "nohup java -cp ${jar_file_name?} ${main_class?} ${mode?} >> ${log_file?} 2>&1 &"
echo "less +F ${log_file?}"
echo
if [ "dev" == "${mode?}" ]; then read -p "must modify watch crontab to match the new log file (they are timestamped)"; fi
echo

# ===========================================================================

exit

