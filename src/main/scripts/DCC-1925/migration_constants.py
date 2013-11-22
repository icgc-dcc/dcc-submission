#!/usr/bin/python
# DCC-1925

DONOR = "donor"
SPECIMEN = "specimen"
SAMPLE = "sample"

SSM_P = "ssm_p"

FILE_TYPES = [SPECIMEN, SSM_P] # TODO: complete

DONOR_ID = "donor_id"
SPECIMEN_ID = "specimen_id"
ANALYZED_SAMPLE_ID = "analyzed_sample_id"

# TODO: check all types are represented (even if with None)
# TODO: read from dictionary instead
# Typically the PK
AFFERENT_RELATIONS[DONOR] = [DONOR_ID]
AFFERENT_RELATIONS[SPECIMEN] = [SPECIMEN_ID]
AFFERENT_RELATIONS[SAMPLE] = [ANALYZED_SAMPLE_ID]

AFFERENT_RELATIONS[SSM_M] = ["analysis_id", ANALYZED_SAMPLE_ID]
AFFERENT_RELATIONS[SSM_P] = None

AFFERENT_RELATIONS[CNSM_M] = ["analysis_id", ANALYZED_SAMPLE_ID]
AFFERENT_RELATIONS[CNSM_P] = ["analysis_id", ANALYZED_SAMPLE_ID, "mutation_id"]
AFFERENT_RELATIONS[CNSM_S] = None

# Typically the FK
SURJECTIVELY_EFFERENT_RELATIONS[DONOR] = None
SURJECTIVELY_EFFERENT_RELATIONS[SPECIMEN] = DONOR_ID
SURJECTIVELY_EFFERENT_RELATIONS[SAMPLE] = SPECIMEN_ID

SURJECTIVELY_EFFERENT_RELATIONS[SSM_M] = None
SURJECTIVELY_EFFERENT_RELATIONS[SSM_P] = ["analysis_id", ANALYZED_SAMPLE_ID]

SURJECTIVELY_EFFERENT_RELATIONS[CNSM_M] = None
SURJECTIVELY_EFFERENT_RELATIONS[CNSM_P] = ["analysis_id", ANALYZED_SAMPLE_ID]
SURJECTIVELY_EFFERENT_RELATIONS[CNSM_S] = None

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

