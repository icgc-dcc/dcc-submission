#!/usr/bin/python
# test: [ "$(echo -e \"a\\tb\\tc\\nd\\te\\tf\\ng\\th\\ti\" | /u/acros/git/develop/src/main/scripts/remove.py 1 b,h)" == "$(echo -e "d\\te\\tf")" ] && echo OK || echo KO
id=131114120246
import sys

field_number = int(sys.argv[1]) # 0-based
csv_exclusion_values = sys.argv[2]

exclusion_values = csv_exclusion_values.split(',')

for line in sys.stdin:
 stripped_line = line.strip('\n')
 fields = stripped_line.split('\t')
 if fields[field_number] not in exclusion_values:
  print stripped_line
