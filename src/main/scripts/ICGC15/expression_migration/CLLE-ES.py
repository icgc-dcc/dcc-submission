#!/usr/bin/python
# /mnt/proxyprod/tmp.py
# /u/acros/tmp.py
# ===========================================================================

exclusion = []
with open("/nfs/dcc_secure/dcc/data/ICGC15/exp/migrated/CLLE-ES.bak/exp_m.txt") as r:
	for line in r:
		line = line.strip('\n')
		fields = line.split('\t')
		if fields[4] == "60":
			key = fields[0] + "|" + fields[1]
			exclusion.append(key)
print exclusion

i = 0
with open("/nfs/dcc_secure/dcc/data/ICGC15/exp/migrated/CLLE-ES/exp_g.txt", 'w') as w:
	with open("/nfs/dcc_secure/dcc/data/ICGC15/exp/migrated/CLLE-ES.bak/exp_g.txt", 'r') as r:
		for line in r:
			line = line.strip('\n')
			fields = line.split('\t')			
			key = fields[0] + "|" + fields[1]
			if key not in exclusion:
				w.write(line + '\n')
			i = i+1
			print i


