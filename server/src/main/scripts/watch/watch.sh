#!/bin/bash
# DCC-735; 130124133056
# monitors keyword appearing in production logs
# 
# install:
#  hwww1-prod:/$ mkdir /srv/webapp/logwatcher && touch /srv/webapp/logwatcher/watch.ignore # put script under /srv/webapp/logwatcher (make sure to +x it)
#
# cron: 
#  MAILTO=""
# 
#  00 * * * *     /srv/webapp/logwatcher/watch.sh /srv/webapp/dcc/log/dcc-server.log /srv/webapp/logwatcher/watch.ignore 0 "anthony.cros@oicr.on.ca,bob.tiernay@oicr.on.ca;shane.wilson@oicr.on.ca" exception # runs every hour and notifies upon failure
#  30 08 * * *    /srv/webapp/logwatcher/watch.sh /srv/webapp/dcc/log/dcc-server.log /srv/webapp/logwatcher/watch.ignore 1 "anthony.cros@oicr.on.ca,bob.tiernay@oicr.on.ca;shane.wilson@oicr.on.ca" exception # runs every day and notifies no matter what
# 
#  30 * * * *     /srv/webapp/logwatcher/watch.sh /srv/webapp/dcc/log/dcc-server.log /srv/webapp/logwatcher/watch.ignore 0 "anthony.cros@oicr.on.ca,bob.tiernay@oicr.on.ca;shane.wilson@oicr.on.ca" error # runs every hour and notifies upon failure
#  45 08 * * *    /srv/webapp/logwatcher/watch.sh /srv/webapp/dcc/log/dcc-server.log /srv/webapp/logwatcher/watch.ignore 1 "anthony.cros@oicr.on.ca,bob.tiernay@oicr.on.ca;shane.wilson@oicr.on.ca" error # runs every day and notifies no matter what

log_file=${1?} && shift
ignore_file=${1?} && shift
send_ok=${1?} && shift
email=${1?} && shift
keyword=$1
keyword=${keyword:=exception}

echo
echo "ignoring:"
echo
cat ${ignore_file?}
echo
echo

# build the awk filter_condition, for instance for:
#  cat watch.ignore
#  a
#  b
# the awk filter_condition produced will be:
#  !/ignoreme/ && !/a/ && !/b/
total=$(wc -l ${ignore_file?} | awk '{print $1}')
declare -i counter=1
filter_condition="!/ignoreme/" # so as to not bother with the first &&
while [ ${counter?} -le ${total?} ]; do
 line=$(head -n${counter?} ${ignore_file?} | tail -n1) # not very efficient
 if [ "$(echo "${line?}" | awk '!/^#/ && !/^$/')" ]; then
  quoted=$(perl -e "print quotemeta('${line?}')")
  filter_condition="${filter_condition?} && !/${quoted?}/"
 fi
 counter=$[counter+1]
done
echo -e "filter_condition:\n\n${filter_condition?}"
echo

# go through log file and search for keyword, while ignore list of known issues/already inspected timestamps
last_offending_lines=$(grep -i ${keyword?} ${log_file?} | awk "${filter_condition?}" | sort -u | tail -n100) # TODO: var for 100

if [ -n "${last_offending_lines?}" ]; then
 # send email if there are offending lines
 echo KO
 echo -e "${log_file?}\n\n${last_offending_lines?}\n\n(last 100 offending lines)" | mail -s "DCC watch - An exception was intercepted in production" ${email?}
 exit 1
else
 # do nothing or send email to ensure script still runs periodically
 echo OK
 if [ "${send_ok?}" == "1" ]; then
  echo -e "everything looks fine" | mail -s "DCC watch - OK" ${email?}
 fi
 exit
fi

