#!/usr/bin/python
# DCC-1925

import os,logging
import migration_constants

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

def get_report_dir(parent_dir):
	return "%s/reports" % parent_dir
def get_data_dir(parent_dir):
	return "%s/data" % parent_dir

def get_report_file(parent_dir, file_type, error_type):
	return "%s/%s-%s.vep" % (get_report_dir(parent_dir), file_type, error_type) # TODO: real path



def split_line(line):
	return line.strip('\n').split('\t')

def read_headers(input_file):
	with open(input_file) as f:
		header = f.readline()
	return split_line(header)

