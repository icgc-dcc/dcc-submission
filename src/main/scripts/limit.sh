#!/bin/bash
# usage: use via dcc.sh only
# prints the max_open_files value for a given host/user/PID based on the given keyword (to infer the PID)

# ===========================================================================

host=${1?} && shift
user=${1?} && shift
keyword=${1?} && shift
echo -ne "\"${host?}\"\t\"${user?}\"\t\"${keyword?}\"" # report arguments

# get PID corresponding to the process matching the keyword provided (typically "tasktracker", "mongod", ...); there should be only 1 match
[ 1 == "$(ps aux | grep \"${keyword?}\" | grep -v bash | grep -v grep | wc -l)" ] || { echo -e "\nERROR: not exactly one matching PID found: $(ps aux | grep \"${keyword?}\")"; exit 1; }
pid=$(ps aux | grep "${keyword?}" | grep -v bash | grep -v grep | awk '{print $2}')

if [ -n "$pid" ]; then # if a pid was found, get the value for max_open_files
 max_open_files=$(cat /proc/${pid?}/limits | grep open | awk '{print $4}')
else # else print something obvious like "?"
 pid="?"
 max_open_files="?"
fi
echo -ne "\t\"${pid?}\"\t\"${max_open_files?}\"" # report values

# ===========================================================================

echo

