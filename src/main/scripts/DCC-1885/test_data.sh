#!/bin/bash -e
# DCC-1885
# usage: ./test_data.sh
# ===========================================================================

cd "$(dirname '$0')"

git checkout -- ../../../../dcc-submission/dcc-submission-server/src/test/resources/fixtures/submission/submission/release1/project.1/*
git checkout -- ../../../../dcc-submission/dcc-submission-server/src/test/resources/fixtures/submission/submission/release1/project.2/*
git checkout -- ../../../../dcc-submission/dcc-submission-server/src/test/resources/fixtures/submission/submission/release1/project.3/*
#git checkout -- ../../../../dcc-submission/dcc-submission-server/src/test/resources/fixtures/submission/submission/release1/project.4/*
#git checkout -- ../../../../dcc-submission/dcc-submission-server/src/test/resources/fixtures/submission/submission/release1/project.5/*
git checkout -- ../../../../dcc-submission/dcc-submission-server/src/test/resources/fixtures/submission/submission/release1/project.6/*
git checkout -- ../../../../dcc-submission/dcc-submission-server/src/test/resources/fixtures/submission/submission/release1/project.7/*

rm -rf /tmp/DCC-1885 || :
mkdir /tmp/DCC-1885

curl -v -XGET -H "Accept: application/json" http://***REMOVED***:5380/ws/nextRelease/dictionary | json_pp > /tmp/DCC-1885/0.7e.json # TODO: use resources
printf '=%.0s' {1..75}

./migration.sh \
 ../../../../dcc-submission/dcc-submission-server/src/test/resources/fixtures/submission/submission/release1/project.1 \
 /tmp/DCC-1885/0.7e.json \
 /tmp/DCC-1885/project.1

./migration.sh \
 ../../../../dcc-submission/dcc-submission-server/src/test/resources/fixtures/submission/submission/release1/project.2 \
 /tmp/DCC-1885/0.7e.json \
 /tmp/DCC-1885/project.2

./migration.sh \
 ../../../../dcc-submission/dcc-submission-server/src/test/resources/fixtures/submission/submission/release1/project.3 \
 /tmp/DCC-1885/0.7e.json \
 /tmp/DCC-1885/project.3

./migration.sh \
 ../../../../dcc-submission/dcc-submission-server/src/test/resources/fixtures/submission/submission/release1/project.4 \
 /tmp/DCC-1885/0.7e.json \
 /tmp/DCC-1885/project.4

./migration.sh \
 ../../../../dcc-submission/dcc-submission-server/src/test/resources/fixtures/submission/submission/release1/project.5 \
 /tmp/DCC-1885/0.7e.json \
 /tmp/DCC-1885/project.5

./migration.sh \
 ../../../../dcc-submission/dcc-submission-server/src/test/resources/fixtures/submission/submission/release1/project.6 \
 /tmp/DCC-1885/0.7e.json \
 /tmp/DCC-1885/project.6

./migration.sh \
 ../../../../dcc-submission/dcc-submission-server/src/test/resources/fixtures/submission/submission/release1/project.7 \
 /tmp/DCC-1885/0.7e.json \
 /tmp/DCC-1885/project.7

#rm -rf /tmp/DCC-1885
