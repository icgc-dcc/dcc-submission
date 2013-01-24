#!/bin/bash
# DCC-735; 130124133056
# cron: 00 * * * *	/var/lib/hdfs/watch.sh /var/lib/hdfs/log/dcc-server-130124120711.log
log_file=${1?}
keyword=$2
keyword=${keyword:=exception}

# build the awk condition, for instance for:
#  cat ignore_list
#  a
#  b
# the awk condition produced will be:
#  !/a/ && !/b/ && !/ ignoreme /
# the ignoreme being because of the trailing '/'
tmp=$(cat /var/lib/hdfs/watch/ignore_list | awk '!/^#/ && !/^$/' | tr "\n" ";" | awk '{gsub(/;/,"/ \\&\\& !/")}1') # see 130124133104
ignore_list="!/${tmp?} ignoreme /"

lines=$(grep -i ${keyword?} ${log_file?} | awk "${ignore_list?}")
if [ -n "${lines?}" ]; then
 echo KO
 echo -e "${log_file?}\n\n${lines?}" | mail -s "DCC - An exception was intercepted in production" anthony.cros@oicr.on.ca
 exit 1
else
 echo OK
 exit
fi
