#!/usr/bin/python
# usage:
#   import relation_utils
#   for outgoing_relation in relation_utils.get_outgoing_relations("ssm_m"):
#      ...

import sys

FK_SCHEMA_OFFSET = 0
PK_SCHEMA_OFFSET = 1
FK_FIELDS_OFFSET = 2
PK_FIELDS_OFFSET = 3
SURJECTIVITY_OFFSET = 4

# ---------------------------------------------------------------------------

dictionary_digest_file = "./dictionary_digest.tsv" # TODO: derive all this from dictionary rather
data = []
first = True
with open(dictionary_digest_file) as f:
	for line in f:
		fields = line.strip('\n').split('\t')
		if not first:
			data.append(fields)
		first = False
# ---------------------------------------------------------------------------

def get_outgoing_relations(schema):
	outgoing = []
	for datum in data:
		if datum[FK_SCHEMA_OFFSET] == schema:
			outgoing_relation = {}
			outgoing_relation["pk_schema"] = datum[PK_SCHEMA_OFFSET]
			outgoing_relation["fk_fields"] = datum[FK_FIELDS_OFFSET].split(',')
			outgoing_relation["pk_fields"] = datum[PK_FIELDS_OFFSET].split(',')
			outgoing.append(outgoing_relation)
	return outgoing
	
# ---------------------------------------------------------------------------

def get_incoming_surjective_relations(schema):
	incoming_sujective = []
	for datum in data:
		if datum[PK_SCHEMA_OFFSET] == schema and datum[SURJECTIVITY_OFFSET] == "true":
			incoming_surjective_relation = {}
			incoming_surjective_relation["fk_schema"] = datum[FK_SCHEMA_OFFSET]
			incoming_surjective_relation["fk_fields"] = datum[FK_FIELDS_OFFSET].split(',')
			incoming_surjective_relation["pk_fields"] = datum[PK_FIELDS_OFFSET].split(',')
			incoming_sujective.append(incoming_surjective_relation)
	return incoming_sujective

# ---------------------------------------------------------------------------

print "# outgoing:"
print "%s\t%s" % ("donor", get_outgoing_relations("donor"))
print "%s\t%s" % ("specimen", get_outgoing_relations("specimen"))
print "%s\t%s" % ("sample", get_outgoing_relations("sample"))
print "%s\t%s" % ("ssm_m", get_outgoing_relations("ssm_m"))
print "%s\t%s" % ("cnsm_s", get_outgoing_relations("cnsm_s"))
print
print "# incoming surjectively:"
print "%s\t%s" % ("donor", get_incoming_surjective_relations("donor"))
print "%s\t%s" % ("specimen", get_incoming_surjective_relations("specimen"))
print "%s\t%s" % ("sample", get_incoming_surjective_relations("sample"))
print "%s\t%s" % ("ssm_m", get_incoming_surjective_relations("ssm_m"))
print "%s\t%s" % ("cnsm_s", get_incoming_surjective_relations("cnsm_s"))

