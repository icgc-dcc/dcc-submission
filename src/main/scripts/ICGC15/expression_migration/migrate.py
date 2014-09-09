#!/usr/bin/python
# Merges the migrated array-based expression data from ICGC14 with the newly submitted RNASeq data from ICGC15
# DCC-2033

import re, glob, gzip, datetime

def is_plain_file(input_file):
	return re.search(".txt$", input_file)
def is_gzip_file(input_file):
	return re.search(".txt.gz$", input_file)
def is_bzip2_file(input_file):
	return re.search(".txt.bz2$", input_file)
def open_file(input_file):
	if is_plain_file(input_file):
		fd = open(input_file)
	elif is_gzip_file(input_file):
		fd = gzip.open(input_file)
	elif is_bzip2_file(input_file):
		fd = bz2.BZ2File(input_file)
	else:
		assert False
	return fd

META_HEADER="analysis_id	analyzed_sample_id	assembly_version	gene_build_version	platform	experimental_protocol	base_calling_algorithm	alignment_algorithm	normalization_algorithm	other_analysis_algorithm	sequencing_strategy	seq_coverage	raw_data_repository	raw_data_accession	note"
GENE_HEADER="analysis_id	analyzed_sample_id	gene_stable_id	gene_chromosome	gene_strand	gene_start	gene_end	normalized_read_count	raw_read_count	normalized_expression_level	fold_change	reference_sample_id	quality_score	probability	is_annotated	verification_status	verification_platform	probeset_id	biological_validation_status	biological_validation_platform	note"

SEQUENCING_STRATEGY_IDX=10
ANALYSIS_ID_IDX=0
ANALYZED_SAMPLE_ID_IDX=1

ARRAY_BASED="29"
RNASEQ="4"

olddir="/nfs/dcc_secure/dcc/data/ICGC15/exp/migrated"
newdir="/hdfs/dcc/icgc/submission/ICGC15"
outputdir="/nfs/dcc_secure/dcc/data/ICGC15/exp/final3"

both = ["BLCA-US", "BRCA-US", "CLLE-ES", "COAD-US", "GBM-US", "HNSC-US", "KIRC-US", "KIRP-US", "LAML-US", "LGG-US", "LIHC-US", "LUAD-US", "LUSC-US", "OV-US", "READ-US", "UCEC-US"]

def get_key(fields):
	analysis_id = fields[ANALYSIS_ID_IDX]
	analyzed_sample_id = fields[ANALYZED_SAMPLE_ID_IDX]
	key = analysis_id + '|' + analyzed_sample_id
	return key

for p in both:
	print "%s, %s" % (p, datetime.datetime.now().strftime("%y%m%d%H%M%S"))

	# ---------------------------------------------------------------------------
	meta_result_file = outputdir + '/' + p + '/' + "exp_m.txt"
	gene_result_file = outputdir + '/' + p + '/' + "exp_g.txt"

	meta_result_data = open(meta_result_file, 'w')
	meta_result_data.write(META_HEADER + '\n')

	gene_result_data = open(gene_result_file, 'w')
	gene_result_data.write(GENE_HEADER + '\n')

	# ---------------------------------------------------------------------------
	meta_old_file = olddir + '/' + p + '/' + "exp_m.txt"

	meta_new_files = glob.glob(newdir + '/' + p + '/' + "exp_m.txt*")
	assert len(meta_new_files) == 1, meta_new_files
	meta_new_file = meta_new_files[0]


	# ---------------------------------------------------------------------------
	gene_old_file = olddir + '/' + p + '/' + "exp_g.txt"

	gene_new_files = glob.glob(newdir + '/' + p + '/' + "exp_g.txt*")
	assert len(gene_new_files)==1, gene_new_files
	gene_new_file = gene_new_files[0]


	# ---------------------------------------------------------------------------
	meta_old_valid_keys = []
	meta_old_invalid_keys = []

	# ---------------------------------------------------------------------------
	meta_old_data = open_file(meta_old_file)
	first = True
	for line in meta_old_data:
		line = line.strip('\n')
		if not first:
			fields = line.split('\t')
			sequencing_strategy = fields[SEQUENCING_STRATEGY_IDX]
			key = get_key(fields)
			if sequencing_strategy == ARRAY_BASED:
				meta_old_valid_keys.append(key)
				meta_result_data.write(line + '\n')
			else:
				assert sequencing_strategy == RNASEQ, sequencing_strategy
				meta_old_invalid_keys.append(key)
		first = False
	meta_old_data.close()

	# ---------------------------------------------------------------------------
	gene_old_data = open_file(gene_old_file)
	first = True
	for line in gene_old_data:
		line = line.strip('\n')
		if not first:
			fields = line.split('\t')
			key = get_key(fields)
			if key in meta_old_valid_keys:
				gene_result_data.write(line + '\n')				
			else:
				assert key in meta_old_invalid_keys, "%s, %s" % (key, meta_old_invalid_keys) # just in case
		first = False
	gene_old_data.close()

	# ---------------------------------------------------------------------------	
	meta_new_data = open_file(meta_new_file)
	first = True
	for line in meta_new_data:
		line = line.strip('\n')
		if not first:
			fields = line.split('\t')
			sequencing_strategy = fields[SEQUENCING_STRATEGY_IDX]			
			assert sequencing_strategy == "4", sequencing_strategy

			key = get_key(fields)
			assert key not in meta_old_valid_keys, "%s, %s" % (key, meta_old_valid_keys)

			meta_result_data.write(line + '\n')
		first = False
	meta_new_data.close()

	# ---------------------------------------------------------------------------
	gene_new_data = open_file(gene_new_file)
	first = True
	for line in gene_new_data:
		line = line.strip('\n')
		if not first:
			fields = line.split('\t')

			key = get_key(fields)
			assert key not in meta_old_valid_keys, "%s, %s" % (key, meta_old_valid_keys)

			gene_result_data.write(line + '\n')
		first = False
	gene_new_data.close()

	# ---------------------------------------------------------------------------

	meta_result_data.close()
	gene_result_data.close()

