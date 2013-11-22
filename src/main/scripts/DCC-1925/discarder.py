#!/usr/bin/python
# DCC-1925
# Usage:
import sys,os,logging,glob
import migration_utils,migration_constants

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
			with open(report_file, 'r') as f:
				lines = f.readlines()
			lines.sort()
			
			# Check no duplicates (see 131121182856)
			assert lines == set(lines)
			
			reports[error_type] = lines
	return reports

# ---------------------------------------------------------------------------

def to_be_skipped(reports, line_number):
	for error_type, line_numbers in reports.iteritems():
		if line_number in line_numbers:
			return True
	return False

# ---------------------------------------------------------------------------

def get_key_values(line):
	fields = migration_utils.split_line(line)
	return [fields[i] for i in key_indices]

# ---------------------------------------------------------------------------

def get_relation_indices(input_file):

	def get_header_indices(headers, keys):
		indices = None
		if keys is not None:
			indices = [headers.index(key) for key in keys]
			assert -1 not in indices
		return indices
		
	headers = migration_utils.read_headers(input_file)
	logging.info("headers: %s" % headers)

	afference_keys = migration_constants.AFFERENT_RELATIONS[file_type]
	surjectively_efference_keys = migration_constants.SURJECTIVELY_EFFERENT_RELATIONS[file_type]
	logging.info("afference_keys: %s" % afference_keys)
	logging.info("surjectively_efference_keys: %s" % surjectively_efference_keys)

	afference_indices = migration_utils.get_header_indices(headers, afference_keys)
	surjectivity_efference_indices = migration_utils.get_header_indices(headers, surjectivity_efference_keys)
	logging.info("afference_indices: %s" % afference_indices)
	logging.info("surjectivity_efference_indices: %s" % surjectivity_efference_indices)

	return afference_indices, surjectivity_efference_indices
				
# ---------------------------------------------------------------------------

def process_file(file_type, input_file):
	reports = get_reports(file_type)
	data_output_file = get_data_output_file()
	afference_file = get_afference_file()
	surjectivity_efference_file = get_surjectivity_efference_file()
	
	logging.info("file_type: %s" % file_type)
	logging.info("input_file: %s" % input_file)
	logging.info("reports: %s" % reports)
	logging.info("data_output_file: %s" % data_output_file)
	logging.info("afference_file: %s" % afference_file)
	logging.info("surjectivity_efference_file: %s" % surjectivity_efference_file)

	afference_indices, surjectivity_efference_indices = get_relation_indices(input_file)

	data = open(data_output_file, 'w')
	afference = open(afference_file, 'w')
	surjective_efference = open(surjectivity_efference_output_file, 'w')
	
	with open(input_file) as f:
		line_number = 0

		for line in f: # Will include header (which will basically never be skipped)
			if to_be_skipped(reports, line_number):
				afference.write(
					'\t'.join(get_key_values(
						line,
						afference_indices))
					+ '\n')
				surjective_efference.write(
					'\t'.join(get_key_values(
						line,
						surjective_efference_indices))
					+ '\n')
			else:
				data.write(line)
			line_number += 1
			
	data.close()
	afference.close()
	surjectivity_efference.close()	
	
# ---------------------------------------------------------------------------

for file_type in migration_utils.FILE_TYPES:
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
