#!/usr/bin/python
# DCC-1925
# Usage:
import sys,os,logging,glob
import utils,migration_utils,migration_constants,discarder_utils

# ===========================================================================

input_dir = sys.argv[1]
output_dir = sys.argv[2]

migration_utils.configure_logging(output_dir, sys.argv[0])
logging.info("input_dir: %s" % input_dir)
logging.info("output_dir: %s" % output_dir)

report_dir = migration_utils.get_report_dir(output_dir)
logging.info("report_dir: %s" % report_dir)

# Reset output dirs
data_dir = migration_utils.get_data_dir(output_dir)
logging.info("data_dir: %s" % data_dir)

if not os.path.isdir(output_dir):
	os.makedirs(output_dir)
if os.path.isdir(data_dir):
	for data_file_name in os.listdir(data_dir):
		os.remove(output_dir + '/' + data_file_name)
else:
	os.makedirs(data_dir)

# ---------------------------------------------------------------------------

def get_reports(file_type):
	reports = {}
	for error_type in migration_utils.ERROR_TYPES:
		report_file = migration_utils.get_report_file(output_dir, file_type, error_type)
		if os.path.isfile(report_file):
			logging.info("Processing report file for: '%s'" % error_type)
			with open(report_file, 'r') as f:
				lines = f.readlines()
			lines.sort()
			
			# Check no duplicates (see 131121182856)
			assert lines == set(lines)
			
			reports[error_type] = lines
		else:
			logging.info("No report file for: '%s'" % error_type)
	return reports

# ---------------------------------------------------------------------------

def to_be_skipped(reports, line_number):
	for error_type, line_numbers in reports.iteritems():
		if line_number in line_numbers:
			return True
	return False

# ---------------------------------------------------------------------------

def get_relation_indices(input_file):		
	headers = utils.read_headers(input_file)
	logging.info("headers: %s" % headers)

	afference_keys = migration_constants.PK[file_type]
	surjective_efference_keys = migration_constants.SURJECTIVE_FK[file_type]
	logging.info("afference_keys: %s" % afference_keys)
	logging.info("surjective_efference_keys: %s" % surjective_efference_keys)

	afference_indices = utils.get_header_indices(headers, afference_keys)
	surjective_efference_indices = utils.get_header_indices(headers, surjective_efference_keys)

	return afference_indices, surjective_efference_indices
				
# ---------------------------------------------------------------------------

def process_file(file_type, input_file):
	logging.info("file_type: %s" % file_type)
	logging.info("input_file: %s" % input_file)
	
	reports = get_reports(file_type)
	logging.info("reports: %s" % reports)
	
	intra_data_output_file = migration_utils.get_intra_data_file(parent_dir, file_type)
	afference_output_file = migration_utils.get_afference_report_file(parent_dir, file_type)
	surjective_efference_output_file = migration_utils.get_surjective_efference_report_file(parent_dir, file_type)
	logging.info("intra_data_output_file: %s" % intra_data_output_file)
	logging.info("afference_output_file: %s" % afference_output_file)
	logging.info("surjective_efference_output_file: %s" % surjective_efference_output_file)

	# Compute report indices
	afference_indices, surjective_efference_indices = get_relation_indices(input_file)
	logging.info("afference_indices: %s" % afference_indices)
	logging.info("surjective_efference_indices: %s" % surjective_efference_indices)

	data = open(intra_data_output_file, 'w')
	afference = open(afference_output_file, 'w')
	surjective_efference = open(surjective_efference_output_file, 'w')
	
	with open(input_file) as f:
		line_number = 0

		for line in f: # Will include header (which will basically never be skipped)
			if to_be_skipped(reports, line_number):
				afference.write(
					'\t'.join(utils.get_tsv_values
						line,
						afference_indices))
					+ '\n')
				surjective_efference.write(
					'\t'.join(utils.get_tsv_values
						line,
						surjective_efference_indices))
					+ '\n')
			else:
				data.write(line)
			line_number += 1
			
	data.close()
	afference.close()
	surjective_efference.close()	
	
# ---------------------------------------------------------------------------

for file_type in migration_utils.FILE_TYPES:
	process_file(file_type, get_input_file(input_dir, file_type))
	
# ===========================================================================
