#!/usr/bin/python
# DCC-1925
# Usage:
import sys,os,logging,glob,gzip
import utils,migration_utils,migration_constants,discarder_utils,relation_utils

# ===========================================================================

input_dir = sys.argv[1]
parent_dir = sys.argv[2]

migration_utils.reset_outputs(parent_dir)
migration_utils.configure_logging(parent_dir, sys.argv[0])
logging.info("input_dir: %s" % input_dir)
logging.info("parent_dir: %s" % parent_dir)

# ---------------------------------------------------------------------------

def get_vep_reports(file_type):
	vep_reports = {}
	for error_type in migration_utils.ERROR_TYPES:
		get_error_report_file = migration_utils.get_error_report_file(parent_dir, file_type, error_type)
		if os.path.isfile(get_error_report_file):
			logging.info("Processing report file for: '%s'" % error_type)
			with open(get_error_report_file, 'r') as f:
				lines = f.readlines()
			lines.sort()
			
			# Check no duplicates (see 131121182856)
			assert lines == set(lines)
			
			vep_reports[error_type] = lines
		else:
			logging.info("No report file for: '%s'" % error_type)
	return vep_reports

# ---------------------------------------------------------------------------

def get_discarding_indices(file_type, original_data_file):
	discarding_fields = relation_utils.get_relation_fields(file_type)
	logging.info("discarding_fields: %s" % discarding_fields)
	
	headers = utils.read_headers(original_data_file):
	logging.info("headers: %s" % headers)
		
	return utils.get_header_indices(headers, discarding_fields)

# ---------------------------------------------------------------------------

def to_be_skipped(vep_reports, line_number):
	for error_type, line_numbers in vep_reports.iteritems():
		if line_number in line_numbers:
			return True
	return False
		
# ---------------------------------------------------------------------------

# This also collects ...TODO
def filter_file(file_type, original_data_file, intra_data_output_file, vep_reports, discarding_indices, discarding_report_output_file):
	discarding_report = open(discarding_report_output_file, 'w')
	
	original_data = migration_utils.open_file(original_data_file)
	
	line_number = 0
	for line in original_data: # Will include header (which will basically never be skipped)
		logging.debug("Considering line: '%s' ('%s')" % (line, line_number))
		discarding_values = utils.get_tsv_values(line, discarding_indices)
		if to_be_skipped(vep_reports, line_number):
			logging.debug("To be skipped")		
			dropped_flag = 1
		else:
			logging.debug("To be kept")
			dropped_flag = 0
			data.write(line)
		report_line = "%s\t%s" % (dropped_flag, '\t'.join(discarding_values))
		logging.debug("Adding report line: '%'" % report_line)
		discarding_report.write(report_line)
		line_number += 1
		
	discarding_report.close()
	original_data.close()

	return intra_data_output_file, afference_values, surjective_efference_values
# ---------------------------------------------------------------------------

def process_file(file_type):
	logging.info("file_type: %s" % file_type)
	
	original_data_file = migration_utils.get_original_data_file(input_dir, file_type)
	if original_data_file is not None:
		logging.info("original_data_file: %s" % original_data_file)
	
		intra_data_output_file = migration_utils.get_intra_data_file(parent_dir, file_type)
		logging.info("intra_data_output_file: %s" % intra_data_output_file)

		vep_reports = get_vep_reports(file_type)
		logging.info("vep_reports: %s" % vep_reports)
		
		discarding_indices = get_discarding_indices(file_type, original_data_file)
		logging.info("discarding_indices: %s" % discarding_indices)

		discarding_report_output_file = migration_utils.get_discarding_report_output_file(parent_dir, file_type)
		logging.info("discarding_report_output_file: %s" % discarding_report_output_file)
	
		filter_file(file_type, original_data_file, intra_data_output_file, vep_reports, discarding_indices, discarding_report_output_file)
	else:
		logging.info("No matches for: '%s'" % file_type)
	
# ---------------------------------------------------------------------------

for file_type in migration_utils.FILE_TYPES:
	process_file(file_type)
	
# ===========================================================================

