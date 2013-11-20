#!/bin/bash -e
# creates a dcc-resources jar
# id: 131118160314
# usage: ./dcc-resources.sh http ***REMOVED*** 5380 0.7a-beta
# notes: MUST have json_pp installed

protocol=${1?} && shift
host=${1?} && shift
port=${1?} && shift
version=${1?} && shift

function unarray() {
 tail -n+2 | head -n-1
}
function split_lines() {
 tr -d '\n' | awk '{gsub(/},   {/,"}\n{")}1'
}

rm -rf /tmp/dcc-resources || :
mkdir /tmp/dcc-resources
curl -H "Accept: application/json" ${protocol?}://${host?}:${port?}/ws/nextRelease/dictionary | json_pp > /tmp/dcc-resources/Dictionary.json
curl -H "Accept: application/json" ${protocol?}://${host?}:${port?}/ws/codeLists | json_pp | unarray | split_lines > /tmp/dcc-resources/CodeList.json

wc -l /tmp/dcc-resources/Dictionary.json
cat /tmp/dcc-resources/Dictionary.json | awk '/"version"/'
wc -l /tmp/dcc-resources/CodeList.json

mkdir -p /tmp/dcc-resources/org/icgc/dcc/resources
mv /tmp/dcc-resources/Dictionary.json /tmp/dcc-resources/CodeList.json /tmp/dcc-resources/org/icgc/dcc/resources/
jar cf /tmp/dcc-resources-${version?}.jar /tmp/dcc-resources/org

ls /tmp/dcc-resources-${version?}.jar
jar tvf /tmp/dcc-resources-${version?}.jar

rm -rf /tmp/dcc-resources

#TODO: pom info
# steps: deploy on artifactory with 
#		GroupId: org.icgc.dcc
#		ArtifactId: dcc-resources
#		if any, remove classifier and add it to version


