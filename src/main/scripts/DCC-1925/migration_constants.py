#!/usr/bin/python
# DCC-1925
# Constants relevant to the migration process

DONOR = "donor"
SPECIMEN = "specimen"
SAMPLE = "sample"

SSM_P = "ssm_p"

FILE_TYPES = [SPECIMEN, SSM_P] # TODO: complete

DONOR_ID = "donor_id"
SPECIMEN_ID = "specimen_id"
ANALYZED_SAMPLE_ID = "analyzed_sample_id"

ANALYSIS_ID = "analysis_id"
MUTATION_ID = "mutation_id"

# TODO: check all types are represented (even if with None)
# TODO: read from dictionary instead

# TODO: ensure the two arrays are consistent (for a given file type, always the same set of fields)
# Typically the PK
PK[DONOR] = [DONOR_ID]
PK[SPECIMEN] = [SPECIMEN_ID]
PK[SAMPLE] = [ANALYZED_SAMPLE_ID]

PK[SSM_M] = [ANALYSIS_ID, ANALYZED_SAMPLE_ID]
PK[SSM_P] = None

PK[CNSM_M] = [ANALYSIS_ID, ANALYZED_SAMPLE_ID]
PK[CNSM_P] = [ANALYSIS_ID, ANALYZED_SAMPLE_ID, MUTATION_ID]
PK[CNSM_S] = None

# Typically the FK
SURJECTIVE_FK[DONOR] = None
SURJECTIVE_FK[SPECIMEN] = DONOR_ID
SURJECTIVE_FK[SAMPLE] = SPECIMEN_ID

SURJECTIVE_FK[SSM_M] = None
SURJECTIVE_FK[SSM_P] = [ANALYSIS_ID, ANALYZED_SAMPLE_ID]

SURJECTIVE_FK[CNSM_M] = None
SURJECTIVE_FK[CNSM_P] = [ANALYSIS_ID, ANALYZED_SAMPLE_ID]
SURJECTIVE_FK[CNSM_S] = None


#ssm_m	sample	analyzed_sample_id
#ssm_m	sample	matched_sample_id	analyzed_sample_id
#ssm_p	ssm_m	analysis_id,analyzed_sample_id
#
#cnsm_p	cnsm_m	analysis_id,analyzed_sample_id
#cnsm_s	cnsm_p	analysis_id,analyzed_sample_id,mutation_id
#cnsm_m	sample	analyzed_sample_id
#cnsm_m	sample	matched_sample_id	analyzed_sample_id
#exp_g	exp_m	analysis_id,analyzed_sample_id
#meth_p	meth_m	analysis_id,analyzed_sample_id
#meth_m	sample	analyzed_sample_id
#meth_m	sample	matched_sample_id	analyzed_sample_id
#mirna_p	mirna_m	analysis_id,analyzed_sample_id
#mirna_s	mirna_p	mirna_seq
#mirna_m	sample	analyzed_sample_id
#pexp_p	pexp_m	analysis_id,analyzed_sample_id
#pexp_m	sample	analyzed_sample_id
#sgv_p	sgv_m	analysis_id,analyzed_sample_id
#sgv_m	sample	analyzed_sample_id
#sgv_m	sample	matched_sample_id	analyzed_sample_id
#
#stsm_p	stsm_m	analysis_id,analyzed_sample_id
#stsm_s	stsm_p	analysis_id,analyzed_sample_id,sv_id,placement
#stsm_m	sample	analyzed_sample_id
#stsm_m	sample	matched_sample_id	analyzed_sample_id
#exp_m	sample	analyzed_sample_id
#jcn_p	jcn_m	analysis_id,analyzed_sample_id
#jcn_m	sample	analyzed_sample_id
#meth_s	meth_p	analysis_id,analyzed_sample_id,methylated_fragment_id
#sample	specimen	specimen_id
#specimen	donor	donor_id


# TODO: prune
STRUCTURALLY_INVALID_ROW_ERROR ="STRUCTURALLY_INVALID_ROW_ERROR"
INVALID_CHARSET_ROW_ERROR ="INVALID_CHARSET_ROW_ERROR"
FORBIDDEN_VALUE_ERROR ="FORBIDDEN_VALUE_ERROR"
RELATION_VALUE_ERROR ="RELATION_VALUE_ERROR"
RELATION_PARENT_VALUE_ERROR ="RELATION_PARENT_VALUE_ERROR"
UNIQUE_VALUE_ERROR ="UNIQUE_VALUE_ERROR"
VALUE_TYPE_ERROR ="VALUE_TYPE_ERROR"
OUT_OF_RANGE_ERROR ="OUT_OF_RANGE_ERROR"
MISSING_VALUE_ERROR ="MISSING_VALUE_ERROR"
CODELIST_ERROR ="CODELIST_ERROR"
DISCRETE_VALUES_ERROR ="DISCRETE_VALUES_ERROR"
REGEX_ERROR ="REGEX_ERROR"
SCRIPT_ERROR ="SCRIPT_ERROR"
TOO_MANY_FILES_ERROR ="TOO_MANY_FILES_ERROR"
RELATION_FILE_ERROR ="RELATION_FILE_ERROR"
REVERSE_RELATION_FILE_ERROR ="REVERSE_RELATION_FILE_ERROR"
COMPRESSION_CODEC_ERROR ="COMPRESSION_CODEC_ERROR"
DUPLICATE_HEADER_ERROR ="DUPLICATE_HEADER_ERROR"
FILE_HEADER_ERROR ="FILE_HEADER_ERROR"
REFERENCE_GENOME_MISMATCH_ERROR ="REFERENCE_GENOME_MISMATCH_ERROR"
REFERENCE_GENOME_INSERTION_ERROR ="REFERENCE_GENOME_INSERTION_ERROR"
TOO_MANY_CONFIDENTIAL_OBSERVATIONS_ERROR ="TOO_MANY_CONFIDENTIAL_OBSERVATIONS_ERROR"

ERROR_TYPES = [ # TODO: prune
		STRUCTURALLY_INVALID_ROW_ERROR,
		INVALID_CHARSET_ROW_ERROR, 
		FORBIDDEN_VALUE_ERROR, 
		RELATION_VALUE_ERROR, 
		RELATION_PARENT_VALUE_ERROR, 
		UNIQUE_VALUE_ERROR, 
		VALUE_TYPE_ERROR, 
		OUT_OF_RANGE_ERROR, 
		MISSING_VALUE_ERROR, 
		CODELIST_ERROR, 
		DISCRETE_VALUES_ERROR, 
		REGEX_ERROR, 
		SCRIPT_ERROR, 
		TOO_MANY_FILES_ERROR, 
		RELATION_FILE_ERROR, 
		REVERSE_RELATION_FILE_ERROR, 
		COMPRESSION_CODEC_ERROR, 
		DUPLICATE_HEADER_ERROR, 
		FILE_HEADER_ERROR, 
		REFERENCE_GENOME_MISMATCH_ERROR, 
		REFERENCE_GENOME_INSERTION_ERROR, 
		TOO_MANY_CONFIDENTIAL_OBSERVATIONS_ERROR
	]

