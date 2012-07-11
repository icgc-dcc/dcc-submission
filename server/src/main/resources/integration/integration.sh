#!/bin/bash -x
[[ "$PWD" =~ "src/main/resources/integration" ]] || { echo "ERROR: must run the script from src/main/resources/integration"; exit 1; }
cp ../seeddata/projects.json .
read -p "please start server with \"mvn exec:java\" then press key"
../seeddata/seed.sh .
curl -v -XPOST http://localhost:5380/ws/releases/initRelease?delete=true -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" --data @initRelease.json
read -p "please copy files on sftp server (sftp -P 5322 admin@localhost) then press key"
curl -v -XGET  http://localhost:5380/ws/nextRelease/queue -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk"
curl -v -XPOST http://localhost:5380/ws/nextRelease/queue -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" --data '["project1", "project2", "project3"]'

