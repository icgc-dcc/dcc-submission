#!/usr/bin/python
# DCC-1925
# Usage:
import sys,os,logging,glob
import utils,migration_utils,migration_constants,discarder_utils

# ===========================================================================

input_dir = sys.argv[1]
parent_dir = sys.argv[2]

migration_utils.configure_logging(parent_dir, sys.argv[0])
logging.info("input_dir: %s" % input_dir)
logging.info("parent_dir: %s" % parent_dir)
migration_utils.reset_outputs(parent_dir)

# ---------------------------------------------------------------------------

def get_reports(file_type):
	reports = {}
	for error_type in migration_utils.ERROR_TYPES:
		get_error_report_file = migration_utils.get_error_report_file(parent_dir, file_type, error_type)
		if os.path.isfile(get_error_report_file):
			logging.info("Processing report file for: '%s'" % error_type)
			with open(get_error_report_file, 'r') as f:
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

# This also collects ...TODO
def filter_file(file_type, input_file):
	reports = get_reports(file_type)
	logging.info("reports: %s" % reports)
	
	intra_data_output_file = migration_utils.get_intra_data_file(parent_dir, file_type)
	data = open(intra_data_output_file, 'w')
	afference_values, surjective_efference_values = [], []
	with open(input_file) as f:
		line_number = 0

		for line in f: # Will include header (which will basically never be skipped)
			logging.debug("Considering line: '%s' ('%s')" % (line, line_number))
			if to_be_skipped(reports, line_number):
				logging.debug("To be skipped")
				afference_values.append(
					'\t'.join(
						utils.get_tsv_values
							line,
							afference_indices))
							
				# This will be further filtered later
				surjective_efference_values.append(
					'\t'.join(
						utils.get_tsv_values
							line,
							afference_indices))				
			else:
				logging.debug("To be kept")
				data.write(line)
			line_number += 1
	data.close()

	return intra_data_output_file, afference_values, surjective_efference_values
	
# ---------------------------------------------------------------------------

# Filters the... TODO
def filter_surjective_efference_values(intra_data_output_file, surjective_efference_indices, surjective_efference_values):

	# 131122152052-scanning
	with open(intra_data_output_file) as f:
		for line in f:
			values = utils.get_tsv_values(line, surjective_efference_indices)
			
			# We remove the key since it's still used (TODO: expand)
			if values in surjective_efference_values:
				surjective_efference_values.remove(values)
				logging.info("Removing '%s' from the list of values to remove" % values)
			else:
				logging.debug("Keeping '%s' in the list of values to remove" % values)

	return surjective_efference_values

# ---------------------------------------------------------------------------

def write_reports(afference_values, filtered_surjective_efference_values):
	afference_report_output_file = migration_utils.get_afference_report_file(parent_dir, file_type)
	surjective_efference_report_output_file = migration_utils.get_surjective_efference_report_file(parent_dir, file_type)
	
	logging.info("afference_output_file: %s" % afference_output_file)
	logging.info("surjective_efference_output_file: %s" % surjective_efference_output_file)

	utils.write_lines(afference_output_file, afference_values)
	utils.write_lines(surjective_efference_output_file, filtered_surjective_efference_values)

# ---------------------------------------------------------------------------

def process_file(file_type):
	logging.info("file_type: %s" % file_type)
	
	input_file = migration_utils.get_original_data_file(input_dir, file_type)
	logging.info("input_file: %s" % input_file)

	# Compute report indices
	intra_data_output_file, afference_indices, surjective_efference_indices = get_relation_indices(input_file)
	logging.info("intra_data_output_file: %s" % intra_data_output_file)
	logging.info("afference_indices: %s" % afference_indices)
	logging.info("surjective_efference_indices: %s" % surjective_efference_indices)

	afference_values, surjective_efference_values = filter_file(file_type, input_file)
	filtered_surjective_efference_values = filter_surjective_efference_values(
		intra_data_output_file,
		surjective_efference_indices,
		surjective_efference_values)
	write_reports(afference_values, filtered_surjective_efference_values)
	
# ---------------------------------------------------------------------------

for file_type in migration_utils.FILE_TYPES:
	process_file(file_type)
	
# ===========================================================================

