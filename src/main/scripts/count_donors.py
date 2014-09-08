#!/usr/bin/python
# usage: count_donors.py /hdfs/dcc/icgc/submission/ICGC15
# ===========================================================================
import sys, os, glob, re, gzip, bz2

release_dir = sys.argv[1]

EXPECTED_HEADER = ["donor_id", "donor_sex", "donor_region_of_residence", "donor_vital_status", "disease_status_last_followup", "donor_relapse_type", "donor_age_at_diagnosis", "donor_age_at_enrollment", "donor_age_at_last_followup", "donor_relapse_interval", "donor_diagnosis_icd10", "donor_tumour_staging_system_at_diagnosis", "donor_tumour_stage_at_diagnosis", "donor_tumour_stage_at_diagnosis_supplemental", "donor_survival_time", "donor_interval_of_last_followup", "donor_notes"]
SIGNED_OFF_PROJECT_KEYS = [ "PACA-AU", "PACA-CA", "PRAD-CA", "RECA-EU", "LICA-FR", "PBCA-DE", "EOPC-DE", "ORCA-IN", "LINC-JP", "LIRI-JP", "THCA-SA", "CLLE-ES", "BOCA-UK", "BRCA-UK", "CMDI-UK", "ESAD-UK", "BLCA-US", "LAML-US", "GBM-US", "LGG-US", "BRCA-US", "CESC-US", "COAD-US", "UCEC-US", "STAD-US", "HNSC-US", "THCA-US", "LIHC-US", "LUAD-US", "LUSC-US", "OV-US", "PAAD-US", "PRAD-US", "KIRC-US", "KIRP-US", "SKCM-US", "READ-US", "OV-AU", "MALY-DE", "PAEN-AU", "LICA-CN", "RECA-CN"]
SIGNED_OFF_PROJECT_KEYS.sort()

# ===========================================================================

def is_plain_file(input_file):
	return re.search(".txt$", input_file)
def is_gzip_file(input_file):
	return re.search(".txt.gz$", input_file)
def is_bzip2_file(input_file):
	return re.search(".txt.bz2$", input_file)
def open_file(input_file):
	if is_plain_file(input_file):
		fd = open(input_file)
	elif is_gzip_file(input_file):
		fd = gzip.open(input_file)
	elif is_bzip2_file(input_file):
		fd = bz2.BZ2File(input_file)
	else:
		assert False
	return fd

# ===========================================================================

total = 0
for signed_off_project_key in SIGNED_OFF_PROJECT_KEYS:
	donors = glob.glob(release_dir + '/' + signed_off_project_key + '/' + "donor*")
	assert len(donors)==1
	donor_file = donors[0]
	data = open_file(donor_file)
	first = True
	
	donor_ids = []
	for line in data.readlines():
		if first:
			header = line.strip().split('\t')
			assert header == EXPECTED_HEADER, "%s: %s != %s" % (signed_off_project_key, header, EXPECTED_HEADER) # ensure expected header
		else:
			assert line
			row = line.strip('\n')
			fields = row.split('\t')
			donor_id = fields[0]
			donor_ids.append(donor_id)
		first = False
	assert len(donor_ids) == len(set(donor_ids)) # ensure no duplicates
	count = len(donor_ids)
	total += count
	print "%s\t%s" % (count, signed_off_project_key)
	data.close()
print total

# ===========================================================================
