#!/usr/bin/python
# DCC-1885
# ===========================================================================
import sys,json

dictionary_file = sys.argv[1] # curl -v -XGET -H "Accept: application/json" http://***REMOVED***:5380/ws/nextRelease/dictionary
target_file_schema_type = sys.argv[2] # as specified in the dictionary: donor, ssm_p, meth_s, ...

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
 for field in fields:
  field_name = actual_field_names[index]
  actual_data_row[field_name] = field
  index = index + 1
 return actual_data_row

# ---------------------------------------------------------------------------

def process_actual_value(actual_value, target_field_name, actual_data_row, target_field_names, extra_field_names):
 processed_value = actual_value

 # ---------------------------------------------------------------------------
 if target_file_schema_type == "ssm_m":
  pass

 # ---------------------------------------------------------------------------
 if target_file_schema_type == "ssm_p":

  # ---------------------------------------------------------------------------

  if target_field_name == "mutated_from_allele" or target_field_name == "mutated_to_allele":
   mutation = actual_data_row["mutation"]
   assert '>' in mutation, "Couldn't find '>' in mutation field: %s" % mutation
   if target_field_name == "mutated_from_allele":
    index = 0
   else:
    index = 1

   processed_value = mutation.split('>')[index]

   # ---------------------------------------------------------------------------

 if not processed_value:
  processed_value = "dummy"

 return processed_value 

# ===========================================================================

with open(dictionary_file) as f:
 dictionary = json.load(f)
target_file_schema = get_target_file_schema(dictionary, target_file_schema_type)
target_field_names = get_target_field_names(target_file_schema)

# ---------------------------------------------------------------------------

first_line = True
extra_field_names = []
for line in sys.stdin:
 assert not '\r' in line # out of safety
 stripped_line = line.strip('\n')
 fields = stripped_line.split('\t')
 if first_line:
  actual_field_names = fields
  for actual_field_name in actual_field_names:
   if actual_field_name not in target_field_names:
    extra_field_names.append(actual_field_name)
  print_expected_header(target_field_names)
  first_line = False
 else:
  actual_data_row = describe_actual_data_row(fields, actual_field_names)
  first_field = True
  for target_field_name in target_field_names:

   if first_field:
    first_field = False
   else:
    sys.stdout.write('\t')

   if target_field_name in actual_data_row:
    actual_value = actual_data_row[target_field_name]
   else:
    actual_value = None

   processed_actual_value = process_actual_value(actual_value, target_field_name, actual_data_row, target_field_names, extra_field_names)
   sys.stdout.write(processed_actual_value) # possibly left unchanged (most cases)

 sys.stdout.write('\n')

# ===========================================================================

