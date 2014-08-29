#!/bin/bash
#
# Copyright 2014(c) The Ontario Institute for Cancer Research. All rights reserved.
#
# Description:
#   Tags all DCC configuration repositories with build tags.
#
# See:
#   https://github.com/icgc-dcc/dcc/tree/develop/src/main/jenkins

echo "Maven version is ${MAVEN_VERSION}"
mkdir dcc-config
cd dcc-config;

git clone git@hproxy-dcc.res.oicr.on.ca:/srv/git/dcc-submission.git; 
cd dcc-submission; 
git tag ${MAVEN_VERSION} -f; 
git push origin :refs/tags/${MAVEN_VERSION}; 
git push --tags
cd ..

git clone git@hproxy-dcc.res.oicr.on.ca:/srv/git/dcc-identifier.git; 
cd dcc-identifier; 
git tag ${MAVEN_VERSION} -f; 
git push origin :refs/tags/${MAVEN_VERSION}; 
git push --tags
cd ..

git clone git@hproxy-dcc.res.oicr.on.ca:/srv/git/dcc-portal.git; 
cd dcc-portal; 
git tag ${MAVEN_VERSION} -f; 
git push origin :refs/tags/${MAVEN_VERSION}; 
git push --tags
cd ..

git clone git@hproxy-dcc.res.oicr.on.ca:/srv/git/dcc-etl.git; 
cd dcc-etl; 
git tag ${MAVEN_VERSION} -f; 
git push origin :refs/tags/${MAVEN_VERSION}; 
git push --tags
cd ..


cd ..
rm -rf dcc-config;