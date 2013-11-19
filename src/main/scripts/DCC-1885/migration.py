#!/usr/bin/python
# DCC-1885
# usage: gzip -c /my/original/ssm__p.txt.gz | ./migration.py /path/to/latest/dictionary.json ssm_p | gzip -cd > /my/migrated/ssm__p.txt.gz
# ===========================================================================
import sys,json

UNKNOWN_FIELD_NAME = "UNKNOWN"
DUMMY_CODE="1"
DATA_MODE_REAL="real"
DATA_MODE_TEST="test"

# ===========================================================================

dictionary_file = sys.argv[1] # curl -v -XGET -H "Accept: application/json" http://***REMOVED***:5380/ws/nextRelease/dictionary
target_file_schema_type = sys.argv[2] # as specified in the dictionary: donor, ssm_p, meth_s, ...
data_mode = sys.argv[3] # either "real" (default) or "test"
if data_mode is None:
 data_mode = DATA_MODE_REAL
assert data_mode == DATA_MODE_REAL or data_mode == DATA_MODE_TEST, "Unknown data mode: '%s', valid values are: '%s'" % (data_mode, [DATA_MODE_REAL, DATA_MODE_TEST])

# ===========================================================================

def get_target_file_schema(dictionary, target_file_schema_type):
 for target_file_schema in dictionary["files"]:
  if target_file_schema["name"] == target_file_schema_type:
   return target_file_schema
   break
 assert False, "Unknown file schema type: %s" % target_file_schema_type

# ---------------------------------------------------------------------------

def get_target_field_names(target_file_schema):
 target_field_names = []
 for field in target_file_schema["fields"]:
  target_field_names.append(field["name"])
 return target_field_names

# ---------------------------------------------------------------------------

def print_expected_header(target_field_names):
 sys.stdout.write('\t'.join(target_field_names))

# ---------------------------------------------------------------------------

def describe_actual_data_row(fields, actual_field_names):
 actual_data_row = {}
 index = 0
 expected_size = len(actual_field_names)
 actual_size = len(fields)
 assert actual_size == expected_size, "Unexpected number of fields encountered: '%s' instead of '%s'" % (actual_size, expected_size) # because submission is supposed to be valid
 for field in fields:
  field_name = actual_field_names[index]
  actual_data_row[field_name] = field
  index = index + 1
 return actual_data_row

# ---------------------------------------------------------------------------

# where most of the conversions take place (on a per row basis)
def process_actual_value(actual_value, target_field_name, actual_data_row, target_field_names, extra_field_names):
 processed_value = actual_value

 # ---------------------------------------------------------------------------
 if data_mode == DATA_MODE_TEST:

  # ...........................................................................
  if target_file_schema_type == "specimen":
   if target_field_name == "cellularity":
    processed_value = DUMMY_CODE
 
  # ...........................................................................
  if target_file_schema_type == "ssm_m":
   if target_field_name in ["assembly_version", "raw_data_repository"]:
    processed_value = DUMMY_CODE
   if target_field_name == "variation_calling_algorithm" or target_field_name == "alignment_algorithm":
    processed_value = "BWA 0.6.2 http://bio-bwa.sourceforge.net"
   if target_field_name == "experimental_protocol":
    processed_value = "Paired End http://www.illumina.com/technology/paired_end_sequencing_assay.ilmn"
   if target_field_name == "base_calling_algorithm":
    processed_value = "CASAVA http://support.illumina.com/sequencing/sequencing_software/casava.ilmn"
 
  # ...........................................................................
  if target_file_schema_type == "ssm_p":
   if target_field_name == "chromosome_strand":
    processed_value = "1"
   if target_field_name == "biological_validation_status":
    processed_value = DUMMY_CODE
   if target_field_name == "biological_validation_platform":
    processed_value = DUMMY_CODE
   if target_field_name == "mutant_allele_read_count":
    processed_value = "1.0"
   if target_field_name == "total_read_count":
    processed_value = "2.0"
   if target_field_name == "expressed_allele":
    processed_value = actual_data_row["tumour_genotype"].split('/')[0]
 
   # ...........................................................................
 
   if target_field_name == "mutated_from_allele" or target_field_name == "mutated_to_allele":
    mutation = actual_data_row["mutation"]
    assert '>' in mutation, "Couldn't find '>' in mutation field: %s" % mutation
    if target_field_name == "mutated_from_allele":
     index = 0
    else:
     index = 1
    processed_value = mutation.split('>')[index]
 
    # ---------------------------------------------------------------------------

 else: # "real" mode
  pass # TODO: this is where migration logic would go

 # ---------------------------------------------------------------------------

 if not processed_value:
  processed_value = "-888"

 return processed_value 

# ===========================================================================

# read dictionary and associated entities
with open(dictionary_file) as f:
 dictionary = json.load(f)
target_file_schema = get_target_file_schema(dictionary, target_file_schema_type)
target_field_names = get_target_field_names(target_file_schema)

# logging
sys.stderr.write("target_file_schema_type: " + target_file_schema_type + '\n')
sys.stderr.write("dictionary_file: " + dictionary_file + '\n')
sys.stderr.write("target_field_names: " + str(target_field_names) + '\n')

# stream each line in standard input and write rows to standard output
first_line = True
extra_field_names = []
for line in sys.stdin:

 assert not '\r' in line # out of safety
 stripped_line = line.strip('\n')
 fields = stripped_line.split('\t')

 # ...........................................................................
 # handle header row
 if first_line:
  actual_field_names = fields

  # gather "unknown" fields (such as "mutation" since it's been removed now)
  for actual_field_name in actual_field_names:
   if actual_field_name not in target_field_names:
    extra_field_names.append(actual_field_name)

  # logging
  sys.stderr.write("actual_field_names: " + str(actual_field_names) + '\n')
  sys.stderr.write("extra_field_names: " + str(extra_field_names) + '\n')

  # print new header
  print_expected_header(target_field_names)

  first_line = False


 # ...........................................................................
 # handle data row
 else:
  actual_data_row = describe_actual_data_row(fields, actual_field_names)
  first_field = True

  # go through target fields and massage data if need be
  for target_field_name in target_field_names:

   # print separator if applicable (all but first field)
   if first_field:
    first_field = False
   else:
    sys.stdout.write('\t')

   # grab existing value if applicable (there wouldn't be any for the new "mutated_from_allele" field for instance)
   if target_field_name in actual_data_row:
    actual_value = actual_data_row[target_field_name]
   else:
    actual_value = None

   # massage value based on the whole row if applicable
   processed_actual_value = process_actual_value(actual_value, target_field_name, actual_data_row, target_field_names, extra_field_names)

   # print new data row
   sys.stdout.write(processed_actual_value) # possibly left unchanged (most cases)

 # ...........................................................................
 # print new line

 sys.stdout.write('\n')

sys.stderr.write('\n')

# ===========================================================================

