#!/usr/bin/python
# DCC-1925
# Usage:
import sys,os,logging,glob
import migration_utils,migration_constants,discarder_utils

# ===========================================================================

input_dir = sys.argv[1]
output_dir = sys.argv[2]

migration_utils.configure_logging(output_dir, sys.argv[0])
logging.info("input_dir: %s" % input_dir)
logging.info("output_dir: %s" % output_dir)

	here

report_dir = migration_utils.get_report_dir(output_dir)
logging.info("report_dir: %s" % report_dir)

# Reset output dirs
data_dir = migration_utils.get_data_dir(output_dir)
logging.info("data_dir: %s" % data_dir)

# TODO: address code duplication
if not os.path.isdir(output_dir):
	os.makedirs(output_dir)
if os.path.isdir(data_dir):
	for data_file_name in os.listdir(data_dir):
		os.remove(output_dir + '/' + data_file_name)
else:
	os.makedirs(data_dir)

# ===========================================================================

def get_report(report_input_file, report_type):
	if os.path.isfile(report_input_file):
		logging.info("Processing report file for: '%s'" % report_type)
		with open(report_input_file, 'r') as f:
			lines = f.readlines()
			lines = [line.split('\t') for line in lines]
			lines.sort() # TODO: necessary?
		return lines
	else:
		logging.info("No report file for: '%s'" % report_type)
		return None

# ---------------------------------------------------------------------------

def to_be_skipped(line, exclusion_value_indices, afference_exclusion_values, surjectivity_efference_exclusion_values):
	values = utils.get_tsv_values(line, exclusion_value_indices)
	if values in afference_exclusion_values or values in surjectivity_efference_exclusion_values:
		return True
	return False

# ---------------------------------------------------------------------------

def get_exclusion_value_indices(input_file):
	headers = utils.read_headers(input_file)
	logging.info("headers: %s" % headers)
	
	keys = migration_constants.PK[file_type]
	logging.info("keys: %s" % afference_keys)
	
	return utils.get_header_indices(headers, keys)

# ---------------------------------------------------------------------------

def process_file(file_type, input_file):
	logging.info("file_type: %s" % file_type)
	logging.info("input_file: %s" % input_file)

	intra_data_input_file = migration_utils.get_intra_data_file(parent_dir, file_type)
	afference_report_input_file = migration_utils.get_afference_report_file(parent_dir, file_type)
	surjectivity_efference_report_input_file = migration_utils.get_surjectivity_efference_report_file(parent_dir, file_type)
	logging.info("intra_data_input_file: %s" % intra_data_input_file)
	logging.info("afference_report_input_file: %s" % afference_report_input_file)
	logging.info("surjectivity_efference_report_input_file: %s" % surjectivity_efference_report_input_file)
	
	afference_report = get_report(afference_report_input_file, "afference"): # TODO: constants
	surjectivity_efference_report = get_report(surjectivity_efference_report_input_file, "surjectivity_efference"): # TODO: constants
	logging.info("afference_report: %s" % afference_report)
	logging.info("surjectivity_efference_report: %s" % surjectivity_efference_report)

	exclusion_value_indices = get_exclusion_value_indices(input_file)
	logging.info("exclusion_value_indices: %s" % exclusion_value_indices)
			
	inter_data_output_file = migration_utils.get_inter_data_file(parent_dir, file_type)
	logging.info("inter_data_output_file: %s" % inter_data_output_file)

	data = open(inter_data_output_file, 'w')
	with open(intra_data_input_file) as f:
		line_number = 0 # TODO: needed?
		for line in f: # Will include header (which will basically never be skipped)
			if not to_be_skipped(line, indices, afference_report, surjectivity_efference_report):
				data.write(line)
			line_number += 1
	data.close()

# ===========================================================================

for file_type in migration_utils.FILE_TYPES:
	process_file(file_type, get_input_file(input_dir, file_type))
	
# ===========================================================================
