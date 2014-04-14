#!/bin/bash
#
# Copyright 2014(c) The Ontario Institute for Cancer Research. All rights reserved.
#
# Description:
#   Builds the reference genome based on https://wiki.oicr.on.ca/display/DCCSOFT/Unify+genome+assembly+build+throughout+the+system
#
# Usage:
#  ./build-reference-genome.sh
#

# Ensembl: 1-22,X,MT
ensemble_url=ftp://ftp.ensembl.org/pub/release-75/fasta/homo_sapiens/dna
sequence_file_base=Homo_sapiens.GRCh37.75.dna.chromosome

# NCBI: Y
ncbi_sequence_file=ftp://ftp.ncbi.nlm.nih.gov/genbank/genomes/Eukaryotes/vertebrates_mammals/Homo_sapiens/GRCh37.p13/Primary_Assembly/assembled_chromosomes/FASTA/chrY.fa.gz

# Build samtools
if [ ! -d samtools ]; then
    mkdir samtools
    git clone -b master https://github.com/samtools/samtools.git;
    cd samtools; make; cd -
fi

if [ ! -d downloads ]; then
    # Download
    mkdir downloads
    wget -P downloads $ensemble_url/$sequence_file_base.{{1..22},{X,MT}}.fa.gz 
    wget $ncbi_sequence_file -O downloads/$sequence_file_base.Y.fa.gz
   
    # Extract
    cd downloads; gunzip *.fa.gz; cd -
fi

if [ ! -d fasta ]; then
    mkdir fasta
fi

for i in {{1..22},{X,MT}}; do echo "$i>"; tail -n+2 downloads/$sequence_file_base.$i.fa ; done > fasta/GRCh37.fasta

# Index
samtools/samtools faidx fasta/GRCh37.fasta
