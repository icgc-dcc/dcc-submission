#!/bin/bash -e
# 
# Executes the dcc submission server
#

bindir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
basedir=`dirname $bindir`
libdir=$basedir/lib
confdir=$basedir/conf
logdir=$basedir/logs

cd $libdir

nohup java -Xmx10g -Dlogback.configurationFile=$confdir/logback.xml -Dlog.dir=$logdir -cp $confdir/:$libdir/dcc-submission-server.jar org.icgc.dcc.Main external $confdir/application.conf >> $logdir/nohup.log 2>&1 &
