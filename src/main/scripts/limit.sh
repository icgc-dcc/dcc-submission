#!/bin/bash
# usage: use via dcc.sh only
# prints the max_open_files value for a given host/user/PID based on the given keyword (to infer the PID)

# ===========================================================================

function get_ps() {
 keyword=${1?}
 ps aux | grep "${keyword?}" | grep -v grep | grep -v sudo | grep -v bash
}

# ===========================================================================

host=${1?} && shift
user=${1?} && shift
keyword=${1?} && shift
echo -ne "\"${host?}\"\t\"${user?}\"\t\"${keyword?}\"" # report arguments

# get PID corresponding to the process matching the keyword provided (typically "tasktracker", "mongod", ...); there should be only 1 match
count=$(get_ps "${keyword?}" | wc -l | awk '{print $1}'); [ "1" == "${count?}" ] || { echo -e "\nERROR: not exactly one matching PID found: |$(get_ps \"${keyword?}\")|"; exit 1; }
pid=$(get_ps "${keyword?}" | awk '{print $2}')

if [ -n "$pid" ]; then # if a pid was found, get the value for max_open_files
 max_open_files=$(cat /proc/${pid?}/limits | grep open | awk '{print $4}')
else # else print something obvious like "?"
 pid="?"
 max_open_files="?"
fi
echo -ne "\t\"${pid?}\"\t\"${max_open_files?}\"" # report values

# ===========================================================================

echo

