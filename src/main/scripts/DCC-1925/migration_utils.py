#!/usr/bin/python
# DCC-1925
# Util methods relevant to the migration process

import os,logging,glob
import utils,migration_constants

# ===========================================================================

# private
def get_report_dir(output_parent_dir):
	return "%s/reports" % output_parent_dir
def get_data_dir(output_parent_dir):
	return "%s/data" % output_parent_dir

# public
def get_intra_report_dir(output_parent_dir):
	return "%s/intra" % get_report_dir(output_parent_dir)
def get_inter_report_dir(output_parent_dir):
	return "%s/inter" % get_report_dir(output_parent_dir)

def get_intra_data_dir(output_parent_dir):
	return "%s/data/intra" % get_data_dir(output_parent_dir)
def get_inter_data_dir(output_parent_dir):
	return "%s/data/inter" % get_data_dir(output_parent_dir)

def get_offset_dir(output_parent_dir):
	return "%s/offsets" % (output_parent_dir)

# private
def get_report_file(output_parent_dir, subdir_name, file_type, error_type):
	subdir = get_intra_dir(output_parent_dir) if subdir_name == "intra" else get_inter_dir(output_parent_dir)
	return "%s/%s-%s.rep" % (subdir, file_type, error_type)
def get_data_file(output_parent_dir, subdir_name, file_type):
	subdir = get_intra_data_dir(output_parent_dir) if subdir_name == "intra" else get_inter_data_dir(output_parent_dir)
	return "%s/%s.txt" % (subdir, file_type.replace('_', '__')) # TODO: constant

# public
def get_log_file(output_parent_dir, script_name):
	return "%s/%s" % (output_parent_dir, os.path.basename(script_name).replace(".py", ".log"))

def get_byte_to_line_offset_mapping_file(output_parent_dir, file_type):
	return "%s/%s.offsets" % (get_offset_dir(output_parent_dir), file_type)

def get_error_report_file(output_parent_dir, file_type, error_type):
	return get_intra_report_file(output_parent_dir, file_type, error_type)
def get_intra_discarding_report_output_file(output_parent_dir, file_type):
	return get_intra_report_file(output_parent_dir, file_type, "intra_discarding") # TODO: constant	
	
def get_afference_report_file(output_parent_dir, file_type):
	return get_inter_report_file(output_parent_dir, file_type, "afference") # TODO: constant
def get_surjectivity_efference_report_file(output_parent_dir, file_type):
	return get_inter_report_file(output_parent_dir, file_type, "surjective_efference") # TODO: constant

def get_intra_data_file(output_parent_dir, file_type):
	return get_data_file(output_parent_dir, "intra", file_type)
def get_inter_data_file(output_parent_dir, file_type):
	return get_data_file(output_parent_dir, "inter", file_type)

def get_original_data_file(input_dir, file_type):
	if '_' in file_type:
		split = file_type.split('_')
		input_file_name_pattern = "%s*_%s*" % (split[0], split[1])
	else:
		input_file_name_pattern = "%s*" % (file_type)
	pattern = input_dir + '/' + input_file_name_pattern
	input_files = glob.glob(pattern)
	
	if input_files:
		assert len(input_files) == 1, pattern
		return input_files[0]
	else:
		return None

# ---------------------------------------------------------------------------

def configure_logging(output_parent_dir, script_name):
	logging.basicConfig(filename=get_log_file(output_parent_dir, script_name), filemode='w', level=logging.INFO)
	logging.getLogger().addHandler(logging.StreamHandler())

# ---------------------------------------------------------------------------

# private
def reset_dir(dir, extension):
	if not os.path.isdir(dir):
		print "Creating: '%s'" % dir
		os.makedirs(dir)
	for file_name in glob.glob(dir + '/' + extension):
		file = dir + '/' + file_name
		if os.path.isfile(file):
			print "Deleting: '%s'" % file
			os.remove(file)

# ---------------------------------------------------------------------------

# Deletes previous logs and empties reports/data dirs
def reset_outputs(output_parent_dir):
	reset_dir(output_parent_dir, "*.log") # TODO: constant
	reset_dir(get_offset_dir(output_parent_dir), "*.offsets")
	reset_dir(get_intra_report_dir(output_parent_dir), "*.rep")
	reset_dir(get_inter_report_dir(output_parent_dir), "*.rep")
	reset_dir(get_intra_data_dir(output_parent_dir), "*.txt")
	reset_dir(get_inter_data_dir(output_parent_dir), "*.txt")
	
# ---------------------------------------------------------------------------

def is_known_error_type(error_type):
	return error_type in migration_constants.ERROR_TYPES
	
# ---------------------------------------------------------------------------

def is_script_error(error_type):
	return error_type == migration_constants.SCRIPT_ERROR

# ===========================================================================

def get_stream_command(input_file):
	stream_command = None
	if utils.is_plain_file(input_file):
		stream_command = "cat"
	elif utils.is_gzip_file(input_file):
		stream_command = "gzip -cd"
	elif utils.is_bzip2_file(input_file):
		stream_command = "bzip2 -cd"
	else:
		assert False
	return stream_command
		
# ---------------------------------------------------------------------------

def open_file(input_file):
	if utils.is_plain_file(input_file):
		fd = open(input_file)
	elif utils.is_gzip_file(input_file):
		fd = gzip.open(input_file)
	elif utils.is_bzip2_file(input_file):
		fd = bz2.BZ2File(input_file)
	else:
		assert False
	return fd
		
# ===========================================================================

