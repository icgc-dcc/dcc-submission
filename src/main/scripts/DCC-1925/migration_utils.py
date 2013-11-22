#!/usr/bin/python
# DCC-1925

import os,logging
import migration_constants

# ---------------------------------------------------------------------------

def reset_outputs(output_dir):

report_dir = migration_utils.get_report_dir(output_dir)
logging.info("report_dir: %s" % report_dir)



	if not os.path.isdir(output_dir):
		os.makedirs(output_dir)
	if os.path.isdir(data_dir):
		for data_file_name in os.listdir(data_dir):
			os.remove(output_dir + '/' + data_file_name)
	else:
		os.makedirs(data_dir)


	get_error_report_file(parent_dir, file_type, error_type)
	get_afference_report_file(parent_dir, file_type)
	get_surjectivity_efference_report_file(parent_dir, file_type)
	get_intra_data_file(parent_dir, file_type)
	get_inter_data_file(parent_dir, file_type)

# ---------------------------------------------------------------------------

def get_log_file(output_dir, script_name):
	return "%s/%s" % (output_dir, os.path.basename(script_name).replace(".py", ".log"))

def configure_logging(output_dir, script_name):
	logging.basicConfig(filename=get_log_file(output_dir, script_name), filemode='w', level=logging.INFO)

# ---------------------------------------------------------------------------

def is_known_error_type(error_type):
	return error_type in migration_constants.ERROR_TYPES
	
def is_script_error(error_type):
	return error_type == migration_constants.SCRIPT_ERROR

# ---------------------------------------------------------------------------

# private
def get_report_dir(parent_dir):
	return "%s/reports" % parent_dir
def get_data_dir(parent_dir):
	return "%s/data" % parent_dir
	
def get_intra_report_dir(parent_dir):
	return "%s/intra" % get_report_dir(parent_dir)
def get_inter_report_dir(parent_dir):
	return "%s/inter" % get_report_dir(parent_dir)

def get_intra_data_dir(parent_dir):
	return "%s/data/intra" % get_data_dir(parent_dir)
def get_inter_data_dir(parent_dir):
	return "%s/data/inter" % get_data_dir(parent_dir)

def get_report_file(parent_dir, subdir_name, file_type, error_type):
	subdir = get_intra_dir(parent_dir) if subdir_name == "intra" else get_inter_dir(parent_dir)
	return "%s/%s-%s.vep" % (subdir, file_type, error_type)
def get_data_file(parent_dir, subdir_name, file_type):
	subdir = get_intra_data_dir(parent_dir) if subdir_name == "intra" else get_inter_data_dir(parent_dir)
	return "%s/%s.txt" % (subdir, file_type.replace('_', '__')) # TODO: constant

# public
def get_error_report_file(parent_dir, file_type, error_type):
	return get_intra_report_file(parent_dir, file_type, error_type)
	
def get_afference_report_file(parent_dir, file_type):
	return get_inter_report_file(parent_dir, file_type, "afference") # TODO: constant
def get_surjectivity_efference_report_file(parent_dir, file_type):
	return get_inter_report_file(parent_dir, file_type, "surjective_efference") # TODO: constant
	
def get_intra_data_file(parent_dir, file_type):
	return get_data_file(parent_dir, "intra", file_type)
def get_inter_data_file(parent_dir, file_type):
	return get_data_file(parent_dir, "inter", file_type)

# ---------------------------------------------------------------------------

def get_original_data_file(input_dir, file_type):
	if '_' in file_type:
		split = file_type.split('_')
		input_file_name_pattern = "%s*__%s*" % (split[0], split[1])
	else:
		input_file_name_pattern = "%s*" % (file_type)
	input_files = glob.glob(input_dir + '/' + input_file_name_pattern)
	assert len(input_files) == 1
	return input_files[0]
	
# ---------------------------------------------------------------------------

