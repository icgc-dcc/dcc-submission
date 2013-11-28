#!/usr/bin/python
# DCC-1925
# Usage:
import sys,os,logging,glob
import migration_utils,migration_constants,discarder_utils

# ===========================================================================

input_dir = sys.argv[1]
parent_dir = sys.argv[2]

migration_utils.reset_outputs(parent_dir)
migration_utils.configure_logging(parent_dir, sys.argv[0])
logging.info("input_dir: %s" % input_dir)
logging.info("output_dir: %s" % parent_dir)

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

def get_exclusion_value_indices(intra_data_input_file):
	headers = utils.read_headers(intra_data_input_file)
	logging.info("headers: %s" % headers)
	
	keys = migration_constants.PK[file_type]
	logging.info("keys: %s" % afference_keys)
	
	return utils.get_header_indices(headers, keys)

# ---------------------------------------------------------------------------

def process_file(file_type, intra_data_input_file):
	
	afference_report_input_file = migration_utils.get_afference_report_file(parent_dir, file_type)
	surjectivity_efference_report_input_file = migration_utils.get_surjectivity_efference_report_file(parent_dir, file_type)
	logging.info("afference_report_input_file: %s" % afference_report_input_file)
	logging.info("surjectivity_efference_report_input_file: %s" % surjectivity_efference_report_input_file)
	
	afference_report = get_report(afference_report_input_file, "afference"): # TODO: constants
	surjectivity_efference_report = get_report(surjectivity_efference_report_input_file, "surjectivity_efference"): # TODO: constants
	logging.info("afference_report: %s" % afference_report)
	logging.info("surjectivity_efference_report: %s" % surjectivity_efference_report)

	exclusion_value_indices = get_exclusion_value_indices(intra_data_input_file)
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
	logging.info("file_type: %s" % file_type)
	
	intra_data_input_file = migration_utils.get_intra_data_file(parent_dir, file_type)
	if intra_data_input_file is not None:
		logging.info("intra_data_input_file: %s" % intra_data_input_file)		
		process_file(file_type, intra_data_input_file)
	else:
		logging.info("No matches for: '%s'" % file_type)

	
# ===========================================================================
