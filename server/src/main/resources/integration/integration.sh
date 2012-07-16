#!/bin/bash -v
# mvn exec:java -Dexec.mainClass="org.icgc.dcc.validation.Main" -Dexec.args="release2 project2"

[[ "$PWD" =~ "src/main/resources/integration" ]] || { echo "ERROR: must run the script from src/main/resources/integration"; exit 1; }
rm -rf /tmp/dcc_root_dir || :

echo
read -p "please make sure the server is started first (\"mvn exec:java\")"
echo

echo
read -p "seed database"
../seeddata/seed.sh .
echo

echo
read -p "create init release"
curl -v -XPUT http://localhost:5380/ws/releases/release1 -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json" --data @initRelease.json
echo

echo
read -p "here you would copy files on to the sftp server with sftp -P 5322 admin@localhost (already copied in our case)"
echo

echo
read -p "get the queue"
curl -v -XGET  http://localhost:5380/ws/nextRelease/queue -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json"
echo

echo
read -p "enqueue projects 1, 2 and 3 for release 1"
curl -v -XPOST http://localhost:5380/ws/nextRelease/queue -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json" --data '["project1", "project2", "project3"]'
echo

echo
read -p "give the server some time to validate"
echo

echo
read -p "check submission states"
echo
curl -v -XGET http://localhost:5380/ws/releases/release1 -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json"
echo
curl -v -XGET http://localhost:5380/ws/releases/release1/submissions/project1 -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json"
echo
curl -v -XGET http://localhost:5380/ws/releases/release1/submissions/project2 -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json"
echo
curl -v -XGET http://localhost:5380/ws/releases/release1/submissions/project3 -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json"
echo

echo
read -p "release 1st release (expect to fail due to no signed off submissions)"
curl -v -XPOST http://localhost:5380/ws/nextRelease/ -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json" --data @nextRelease.json
echo

echo
read -p "sign off on project 1"
curl -v -XPOST http://localhost:5380/ws/nextRelease/signed -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json" --data @projectkeys.json
echo

echo
read -p "release 1st release (expect success)"
curl -v -XPOST http://localhost:5380/ws/nextRelease/ -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json" --data @nextRelease.json
echo

echo
read -p "release 1st release (expect to fail due to duplicate release)"
curl -v -XPOST http://localhost:5380/ws/nextRelease/ -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json" --data @nextRelease.json
echo

echo
read -p "check releases states"
echo
curl -v -XGET http://localhost:5380/ws/releases/release1 -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json"
echo
curl -v -XGET http://localhost:5380/ws/releases/release2 -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json"
echo

echo
read -p "here you would copy files on to the sftp server with sftp -P 5322 admin@localhost (already copied in our case)"
echo

echo
read -p "enqueue project 2 for release 2"
curl -v -XPOST http://localhost:5380/ws/nextRelease/queue -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json" --data '["project2"]'
echo

echo
read -p "give the server some time to validate"
echo

echo
read -p "check submission states"
echo
curl -v -XGET http://localhost:5380/ws/releases/release2 -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json"
echo
curl -v -XGET http://localhost:5380/ws/releases/release2/submissions/project2 -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json"
echo

echo
read -p "observe expected error"
echo
cat /tmp/dcc_root_dir/release2/project2/.validation/donor.internal.tsv
echo

echo
read -p "release 2nd release"
curl -v -XPOST http://localhost:5380/ws/nextRelease/ -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json" --data @nextRelease2.json
echo

echo
read -p "check releases states"
echo
curl -v -XGET http://localhost:5380/ws/releases/release2 -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json"
echo
curl -v -XGET http://localhost:5380/ws/releases/release3 -H "Authorization: X-DCC-Auth YWRtaW46YWRtaW5zcGFzc3dk" -H "Content-Type: application/json" -H "Accept: application/json"
echo

read -p "done"'!'

