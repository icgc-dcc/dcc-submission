#!/usr/bin/python
# DCC-1925
# Usage: /mnt/proxyprod/git/branches/migration_testing/src/main/scripts/DCC-1925/vep.py ./dcc-submission/dcc-submission-validator/target/test-classes/fixtures/validation/external/error/fk_1/.validation /tmp/DCC-1925
# Assumption: json documents have no separators, though they may contain newlines: { ... doc 1 ... }{ ... doc 2 ...}
# Report files will be unsorted (but read in memory later on anyway); TODO: expand
# 131121182856 - There should be no duplicates in the report files (no line number reported twice in the same file)
import sys,os,logging,json,subprocess
import migration_utils

# ---------------------------------------------------------------------------

input_dir = sys.argv[1]
parent_dir = sys.argv[2]

migration_utils.configure_logging(parent_dir, sys.argv[0])
logging.info("input_dir: %s" % input_dir)
logging.info("parent_dir: %s" % parent_dir)
migration_utils.reset_outputs(output_dir)


# ---------------------------------------------------------------------------

# Example of serialized TupleError(s):
# {
#   "offset" : 2,
#   "errors" : [ {
#     "type" : "CODELIST_ERROR",
#     "columnNames" : [ "donor_vital_status" ],
#     "number" : 0,
#     "value" : "3",
#     "line" : 2,
#     "parameters" : { }
#   } ]
# }{ ...
def process_doc(file_type, doc):
	json_doc = json.loads(doc)
	byte_offset = long(json_doc["offset"])
	line_offset = convert_line_number(input_file, byte_offset)
	
	error_types_encountered = []
	for error in json_doc["errors"]:
		error_type = error["type"]
		assert migration_utils.is_known_error_type(error_type)
		number = int(error["number"])
		assert number == 0 or migration_utils.is_script_error(error_type)
		
		# We only need to report one such error type per document/line
		if error_type not in error_types_encountered:
			error_types_encountered.append(error_type)

			error_report_file = migration_utils.get_error_report_file(parent_dir, file_type, error_type)
			
			with open(error_report_file, 'a') as f:
				f.write(str(line_offset) + '\n')

# ---------------------------------------------------------------------------

def process_tuple_error_file(file_type, tuple_errors_input_file):

	error_count = 0
	doc = ""   
	for part_file in glob.glob(tuple_errors_input_file + '/part*.gz'):
		with gzip.open(part_file, 'r') as f:
			for line in f:
				line = line.strip()
			
				if "}{" not in line:
					doc = "%s%s" % (doc, line) # TODO: improve?
				else:
					split = line.split("}{")
					assert len(split) == 2
					doc = "%s%s}" % (doc, split[0])
				
					# process non-last document (including first)
					process_doc(file_type, doc.replace('\n', ''))
					error_count += 1
				
					doc = "{%s" % split[1] # next doc
				
	# process last document
	process_doc(file_type, doc)
	error_count = error_count + 1
	logging.info("error_count: %s" % error_count)
	
	assert error_count > 0 or os.path.getsize(input_file) == 0
 
# ---------------------------------------------------------------------------

def get_offset_mapping(original_data_input_file)
	byte_to_line_offset_mapping_file = get_byte_to_line_offset_mapping_file()
	subprocess.Popen("grep -obn '^.' %s | awk -F':' '{print $2 \"\t\" ($1-1)}' > %s" % (original_data_input_file, byte_to_line_offset_mapping_file), shell=True) # TODO: explain trick
	offset_mapping = {}
	with open(byte_to_line_offset_mapping_file) as f:
		for line in f: # asmpt-131122173214
			key_value_pair = utils.split_line(line)
			byte_offset = key_value_pair[0]
			line_offset = key_value_pair[1]
			offset_mapping[byte_offset] = line_offset
			
	# TODO: consider deleting temporary file?
	
	return offset_mapping

# ---------------------------------------------------------------------------

for file_type in migration_constants.FILE_TYPES:
	tuple_errors_input_file_name = "%s.internal--errors.json" % file_type
	if tuple_errors_input_file_name in os.listdir(input_dir):
	
		original_data_input_file = migration_utils.get_original_data_file(input_dir, file_type)
		logging.info("original_data_input_file: %s" % original_data_input_file)
		assert os.path.isfile(original_data_input_file) # Must exist if there is a corresponding tuple error file
	
		# Convert byte offsets to line offsets
		offset_mapping = get_offset_mapping(original_data_input_file, )
		logging.info("Offset mapping: '%s'", offset_mapping) # TODO: limit?

		process_tuple_error_file(file_type, input_dir + '/' + tuple_errors_input_file_name)
		
# ===========================================================================

