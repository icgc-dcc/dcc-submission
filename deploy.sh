#!/bin/bash -e
# see DCC-499
#
# usage:
# - production  mode: ./deploy.sh production  hwww.res.oicr.on.ca
# - development mode: ./deploy.sh development hwww2.res.oicr.on.ca
#
# notes:
# - based on https://wiki.oicr.on.ca/display/DCCSOFT/Standard+operating+procedures#Standardoperatingprocedures-SOPforDeployingtheserver (which also links to this script now)
# - TODO: fix wiki + link to this script + comment about not runnign cake
# - split in two scripts: one local, one remote

# export this file, or wget it from github link: 
# ssh+wget https://github.com/icgc-dcc/data-submission/raw/master/deploy.sh

exit_code=0

mode=${1?}
server=${2?}
echo "mode=\"${mode?}\""
echo "server=\"${server?}\""

server_lib_dir="/tmp/lib" # must be absolute path
server_client_dir="/tmp/client" # must be absolute path
server_log_dir="/tmp/log" # must be absolute path
echo "server_lib_dir=\"${server_lib_dir?}\""
echo "server_client_dir=\"${server_client_dir?}\""
echo "server_log_dir=\"${server_log_dir?}\""

timestamp=$(date "+%y%m%d%H%M%S")
echo "timestamp=\"${timestamp?}\""

# ---------------------------------------------------------------------------
# basic checks

# check git
hash git 2>&- || { echo "ERROR: git is not available"; exit $[exit_code+1]; }

# check mvn
hash mvn 2>&- || { echo "ERROR: mvn is not available"; exit $[exit_code+1]; }

function check_exit_code() {
  exit_code=${1?}
  [ ${exit_code?} == 0 ] && echoo1 "OK!" || echoo2 "ERROR: ${CODE?}"
}

# ===========================================================================

#TODO: check git installed

git clone git@github.com:icgc-dcc/data-submission.git ${dir?} #TODO: full path?
cd ${dir?}
git checkout master
mvn assembly:assembly
cd ./client
run `brunch build`
cd ..

pom_file="./server/pom.xml"
artifact_id=$(xpath -e "//project/artifactId/text()" ${pom_file?})
version=$(xpath -e "//project/parent/version/text()" ${pom_file?})
echo "artifact_id=\"${artifact_id?}\""
echo "version=\"${version?}\""

mkdir -p ${server_lib_dir?} ${server_client_dir?}
cp server/target/${artifact_id?}-${version?}.jar ${server_lib_dir?}
cp -r client/public ${server_client_dir?}
cd ${server_lib_dir?}

log_base="${artifact_id?}-${timestamp?}"
output_log_file="${server_log_dir?}/${log_base?}.out"
error_log_file="${server_log_dir?}/${log_base?}.err"
cd ${server_lib_dir?}
nohup java -cp ${artifact_id?}-${version?}.jar org.icgc.dcc.Main prod \
 >${output_log_file?} 2>${error_log_file?} & # no need to start the client server (see comment in https://wiki.oicr.on.ca/display/DCCSOFT/Development+Notes)
pid=$!
echo "pid=\"${pid?}\""

echo "---------------------------------------------------------------------------"
echo -e "server started as pid \"${pid?}\", monitor with:\n tail -f ${output_log_file?}\n tail -f ${error_log_file?}"

# ===========================================================================

exit ${exit_code?} # should still be 0 at this stage (else we exited already) :)

