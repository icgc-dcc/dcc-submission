#!/bin/bash

################################################################################
# Provisions a cluster and execute submission validations for specific projects
#
# Usage:
#    wrapper.sh <project 1> <project 2> ... <project n>
#
# 
# Notes: 
# - The <server> parameter need to match the master floatip as specified
# in the vagrant launcher configuration file. 
#
#
################################################################################





working_dir="<path_to_working_directory>"
notification_email="<user@example.com>"

# openstack
server="<master_node_ip>"
server_port="<openstack_DCC_submission_application_port>"

# meta
metadata_server="<DCC_submission_dev_server>"
metadata_server_port="<DCC_submission_dev_server_port>"
dictionary_version="<DCC_submission_dictionary_version>"
initial_release_name="validation_test"
codelists_file="codelist.json"
dictionary_file="dictionary.json"

# authentication
user_name="<DCC_submission_user_name>"
user_passwd_file="<path_to_DCC_submission_user_password>"
user_key="<path_to_DCC_submission_authentication_key>"
openstack_key="<path_to_openstack_pem_key>"


# patch
application_conf_path="<path_to_application.conf>"

# remote dir
install_path="<path_to_install>"
openstack_genome_dir="<dir_to_install_genome_reference>"

# local dir
ftp_dir="<dir_to_submission_files>"
genome_dir="<dir_to_genome_reference_GRCh37>"


# Derived - don't touch
user_passwd=$(<${user_passwd_file})
dcc_auth=$(echo -n ${user_name?}:${user_passwd?} | base64)




# Make script more readable
_scp() {
   scp -o StrictHostkeyChecking=no -o UserKnownHostsFile=/dev/null "$@"
}

_ssh() {
   ssh -o StrictHostkeyChecking=no -o UserKnownHostsFile=/dev/null "$@"
}

_curl() {
   curl -H "Accept: application/json" -H "Content-Type: application/json"  -H "Authorization: X-DCC-Auth $(echo -n ${user_name?}:${user_passwd?} | base64)" "$@"
}

_log() {
  date=`date`
  echo "$date -- $@" >> logs/runlog.txt
}


################################################################################
# Stage 1: Provision cluster nodes 
# Note that we are launch outside of the openstack cluster, as we cannot
# access the data files from inside. As such, we are only using the first parts
# (setting up the servers) of the provisioning script. 
################################################################################
mkdir -p logs
_log ""
_log "=== Starting Validation Test [ "$@" ] ==="
if [ ! -d ${working_dir} ]; then
   #VAGRANT_LOG=DEBUG perl vagrant_cluster_launch_fix.pl --use-openstack --working-dir=${working_dir} --config-file=cluster.json
   VAGRANT_LOG=DEBUG perl vagrant_cluster_launch.pl --use-openstack --working-dir=${working_dir} --config-file=cluster.json
else
   # Check if server is reachable.
   _ssh -i ${openstack_key} ubuntu@${server} "ls" && echo "OK" || (echo "VM ${server} appear to exist but cannot be reached" && exit)
fi



################################################################################
# Stage 2: Setup validation server packages
################################################################################
_log "start setting up submission server"
echo "Fetching validation server"
_ssh -i ${openstack_key} ubuntu@${server} /bin/bash <<EOF
	sudo mkdir -p ${install_path}
	cd ${install_path}

	sudo ${isntall_path}/dcc-submission-server/bin/dcc-submission-server remove
   sudo sleep 5
   sudo echo "Removing dcc submission server"
   sudo rm -rf ${install_path}/*
	sudo mkdir -p dcc-submission-server
   sudo sleep 5

	sudo wget http://seqwaremaven.oicr.on.ca/artifactory/dcc-release/org/icgc/dcc/dcc-submission-server/2.1.8/dcc-submission-server-2.1.8-dist.tar.gz
	sudo tar zxf dcc-submission-server-2.1.8-dist.tar.gz --strip 1 -C ${install_path}/dcc-submission-server
EOF


# FIXME: see if we can just direclty copy to submission server
echo "Start copying configuration files"
_scp -i ${openstack_key} ${application_conf_path} ubuntu@${server}:/tmp/application.conf


echo "Start copying reference genome"
_scp -i ${openstack_key} ${genome_dir}/GRCh37.fasta.fai ubuntu@${server}:${openstack_genome_dir}/GRCh37.fasta.fai
_scp -i ${openstack_key} ${genome_dir}/GRCh37.fasta ubuntu@${server}:${openstack_genome_dir}/GRCh37.fasta


echo "Starting validation server..."
_ssh -i ${openstack_key} ubuntu@${server} /bin/bash <<EOF
	sudo cp /tmp/application.conf ${install_path}/dcc-submission-server/conf/application.conf
	sudo ${install_path}/dcc-submission-server/bin/install -s
EOF
sleep 5



################################################################################
# Stage 3: Setup projects and the validation environment
################################################################################
echo ">>> Dropping mongo icgc-dev database"
_log "setting up dictionary/codelist"
_ssh -i ${openstack_key} ubuntu@${server} '/usr/bin/mongo icgc-dev --eval "db.dropDatabase()"'
_ssh -i ${openstack_key} ubuntu@${server} '/usr/bin/mongo icgc-local --eval "db.dropDatabase()"'


echo ">>> Removing /tmp/submission/*"
ssh -i ${openstack_key} -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null ubuntu@${server} <<EOF
	hadoop fs -rm -r /tmp/submission/*
	sudo rm -rf /tmp/submission/*
EOF


# download origin dictionary and ensure state is OPENED
echo ">>> Getting dictionary ${dictionary_version} from ${metadata_server}"
curl ${metadata_server?}:${metadata_server_port?}/ws/dictionaries/${dictionary_version} -H "Accept: application/json"  > ${dictionary_file?} && echo "OK" || echo "KO"

# download origin codelists
echo ">>> Getting codelists from ${metadata_server}"
curl ${metadata_server?}:${metadata_server_port?}/ws/codeLists -H "Accept: application/json" > ${codelists_file?} && echo "OK" || echo "KO"


# upload codelists to destination
echo ">>> Uploading codelists"
_curl -XPOST ${server?}:${server_port?}/ws/codeLists --data @${codelists_file?} && echo "OK" || echo "KO"


# upload dictionary to destination
echo ">>> Uploading dictionary"
_curl -vvv -XPOST ${server?}:${server_port?}/ws/dictionaries --data @${dictionary_file?} && echo "OK" || echo "KO"


echo ">>> Creating initial release: ${initial_release_name}"
_curl -v -XPUT ${server?}:${server_port?}/ws/releases --data "{ \"name\" : \"${initial_release_name?}\", \"dictionaryVersion\" : \"${dictionary_version?}\", \"submissions\": [], \"state\" : \"OPENED\" }"


# Create projects and ftp files
_log "sending data files"
for proj in "$@"
do
   # add a project
   echo ">>> Adding project ${proj}"
   _curl -XPOST ${server?}:${server_port}/ws/projects --data "{\"key\": \"${proj}\", \"name\": \"${proj}\", \"alias\": \"${proj}\", \"users\": [\"guest\"], \"groups\": []}" 

   # upload submission files
   # TODO: check dir exists
   echo ">>> FTP files fro project ${proj}"
   sftp -i ${user_key} -P5322 -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no ${user_name}@${server} <<EOF
   cd ${proj}
   lcd ${ftp_dir}/${proj}
   put *
   bye
EOF
done




################################################################################
# Stage 4: Launch validation tests. 
# Hand it off to a python script to better handle data and control flow of the
# submission check
################################################################################
echo ">>> Requesting projects to be queued"
_log "Checking validation"
for proj in "$@"
do
   echo ">>> Sending queue request for ${proj}"
   _curl -v -XPOST ${server?}:${server_port}/ws/nextRelease/queue --data "[{\"key\": \"${proj}\", \"emails\": [\"user@example.com\"]}]" && echo "OK" || echo "Failed to send validation request"
done

echo "Starting polling for validation states"
python checkValidation.py "http://${server}:${server_port}" ${initial_release_name} ${dcc_auth}




################################################################################
# Stage 5: Send and store summary reports
################################################################################
_log "Gathering summary and reports"
cat summary.txt | /usr/bin/mailx -s "DCC Validation Test" "${notification_email}"


log_folder=`date +"%Y-%m-%d_%H_%M"`
mkdir logs/${log_folder}

for proj in "$@"
do
   echo "copy validation files for ${proj}"
   _ssh -i ${openstack_key} ubuntu@${server} /bin/bash <<EOF
      sudo rm -rf /tmp/${proj}_validation
      sudo hadoop fs -copyToLocal /tmp/submission/${initial_release_name}/${proj}/.validation /tmp/${proj}_validation
EOF

  _scp -r -i ${openstack_key} ubuntu@${server}:/tmp/${proj}_validation logs/${log_folder}/${proj}_validation
done


echo "copy server logs"
_scp -i ${openstack_key} ubuntu@${server}:${install_path}/dcc-submission-server/logs/dcc-server.log logs/${log_folder}/dcc-server.log


################################################################################
# Stage 6: Tear-down VMs and free up resources
################################################################################
cd ${working_dir}
for vm in "./"*
do
   echo ${vm}
   cd ${vm}
   vagrant destroy
   cd ../
done
cd ../
rm -rf ${working_dir}

echo "Done!!!"


