#!/bin/bash -x
[[ "$PWD" =~ "src/main/resources/integration" ]] || { echo "ERROR: must run the script from src/main/resources/integration"; exit 1; }
rm -rf /tmp/dcc_root_dir || :
cp ../seeddata/projects.json .
read -p "please start server with \"mvn exec:java\" then press key"
../seeddata/seed.sh .
curl -v -XPUT http://localhost:5380/ws/releases/release1 -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" --data @initRelease.json
read -p "here you would copy files on to the sftp server with sftp -P 5322 admin@localhost (already copied in our case)"
curl -v -XGET  http://localhost:5380/ws/nextRelease/queue -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk"
#curl -v -XPOST http://localhost:5380/ws/nextRelease/queue -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" --data '["project1", "project2", "project3"]'
