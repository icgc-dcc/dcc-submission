#!/usr/bin/python

# TODO: create and move to utils.py
def split_line(line):
	return line.strip('\n').split('\t')

def read_headers(input_file):
	with open(input_file) as f:
		header = f.readline()
	return split_line(header)

def get_tsv_values(line, indices):
	fields = migration_utils.split_line(line)
	return [fields[i] for i in indices]

def get_header_indices(headers, subset):
	indices = None
	if keys is not None:
		indices = [headers.index(header) for header in subset]
		assert -1 not in indices
	return indices

def write_lines(output_file, lines):
	with open(output_file, 'w') as f:
		for line in lines:
			afference.write(lines + '\n')

