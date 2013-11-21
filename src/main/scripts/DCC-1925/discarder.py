#!/usr/bin/python
# DCC-1925
# Usage:
import sys,os,glob
import migration_utils

# ===========================================================================

input_dir = sys.argv[1]
report_dir = sys.argv[2]
output_dir = sys.argv[3]

print "input_dir: %s" % input_dir
print "report_dir: %s" % report_dir
print "output_dir: %s" % output_dir

# ---------------------------------------------------------------------------

def get_reports(file_type):
	reports = {}
	for error_type in migration_utils.ERROR_TYPES:
		report_file = migration_utils.get_report_file(output_dir, file_type, error_type)
		if os.path.isfile(report_file):
			with open(report_file, 'r') as f:
				lines = f.readlines()
			lines.sort()
			
			# Check no duplicates (see 131121182856)
			assert lines == set(lines)
			
			reports[error_type] = lines
	return reports
			
# ---------------------------------------------------------------------------

def process_file(file_type, input_file):
	reports = get_reports(file_type)
	pass
	
# ---------------------------------------------------------------------------

file_types = ["ssm_p"] # TODO: complete
for file_type in file_types:
	if '_' in file_type:
		split = file_type.split('_')
		input_file_name_pattern = "%s*__%s*" % (split[0], split[1])
	else:
		input_file_name_pattern = "%s*" % (file_type)
	input_files = glob.glob(input_dir + '/' + input_file_name_pattern)
	assert len(input_files) == 1
	input_file = input_files[0]
	
	process_file(file_type, input_file)
	
# ===========================================================================
