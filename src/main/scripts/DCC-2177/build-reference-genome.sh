#!/bin/bash
#
# Copyright 2014(c) The Ontario Institute for Cancer Research. All rights reserved.
#
# Description:
#   Builds and deploys the reference genome based on https://wiki.oicr.on.ca/display/DCCSOFT/Unify+genome+assembly+build+throughout+the+system
#
# Notes:
#   Requires ncurses-dev zlib1g-dev
# 
# Usage:
#   ./build-reference-genome.sh -u <username> -p -v <version>
#
# Download latest:
#	curl -g 'http://seqwaremaven.oicr.on.ca/artifactory/dcc-dependencies/org/icgc/dcc/dcc-reference-genome/[RELEASE]/dcc-reference-genome-[RELEASE].tar.gz' | tar zvx
#

usage(){
cat << EOF
usage: $0 options

This script downloads, formats, builds and deploys the GRCh37.fasta and GRCh37.fasta.fai tarball to Artifactory

see https://jira.oicr.on.ca/browse/DCC-2177 for details

OPTIONS:
   -h      Show this message
   -v      Artifactory version number (e.g. 75.v1)
   -u      Artifactory username
   -p      Artifactory password
EOF
}

log(){
  echo `date` -  $1 | tee -a $logdir/install.log
}

version=
username=
while getopts “hpv:u:” OPTION
do
  case $OPTION in
         h)
             usage
             exit 1
             ;;
         v)
             version=$OPTARG
             ;;
         u)
             username=$OPTARG
             ;;
         p)
             read -s -p "Enter Password: " password
             ;;
         ?)
             usage
             exit
             ;;
     esac
done

if [[ -z $version ]] || [[ -z $username ]] || [[ -z $password ]]
then
  usage
  exit 1
fi

# Maven artifact location
artifact_server=http://seqwaremaven.oicr.on.ca/artifactory
artifact_repository=dcc-dependencies
artifact_name=dcc-reference-genome
artifact_path=org/icgc/dcc/$artifact_name
artifact_version=GRCh37.$version
archive_file=$artifact_name-$artifact_version.tar.gz

# FASTA files
fasta_dir=fasta
fasta_readme=README.${artifact_version}.txt
fasta_file=${artifact_version}.fasta
fasta_fai_file=${fasta_file}.fai
fasta_dict_file=${artifact_version}.dict

# Picard tools
picard_version=1.111
picard_dist="http://softlayer-dal.dl.sourceforge.net/project/picard/picard-tools/$picard_version/picard-tools-$picard_version.zip"
picard_jar="picard/picard-tools-$picard_version/CreateSequenceDictionary.jar"

# Deploy
source=fasta/$archive_file
target=$artifact_server/$artifact_repository/$artifact_path/$artifact_version/$archive_file

# Ensembl: 1-22,X,MT
ensemble_url=ftp://ftp.ensembl.org/pub/release-75/fasta/homo_sapiens/dna
sequence_file_base=Homo_sapiens.GRCh37.75.dna.chromosome

# NCBI: Y
ncbi_sequence_file=ftp://ftp.ncbi.nlm.nih.gov/genbank/genomes/Eukaryotes/vertebrates_mammals/Homo_sapiens/GRCh37.p13/Primary_Assembly/assembled_chromosomes/FASTA/chrY.fa.gz

# Build samtools
if [ ! -d samtools ]; then
	echo "Building samtools..."
    mkdir samtools
    git clone -b master https://github.com/samtools/samtools.git;
    cd samtools; make; cd -
fi

# Download picard
if [ ! -d picard ]; then
	echo "Downloading picard..."
	mkdir picard
	cd picard
    wget -O picard.zip "http://sourceforge.net/projects/picard/files/latest/download?source=files"
    unzip picard.zip
    cd -
fi

if [ ! -d downloads ]; then
    # Download
	echo "Downloading sequences..."
    mkdir downloads
    wget -P downloads $ensemble_url/$sequence_file_base.{{1..22},{X,MT}}.fa.gz 
    wget $ncbi_sequence_file -O downloads/$sequence_file_base.Y.fa.gz
   
    # Extract
    cd downloads; gunzip *.fa.gz; cd -
fi

if [ ! -d fasta ]; then
    mkdir fasta
fi

echo -e "\nBuilding FASTA file..."
for i in {{1..22},{X,Y,MT}}; do 
	# Normalize header
	echo ">$i"
	
	if [ "$i" == "Y" ]
	then
		# Reformat line length
		tail -n+2 downloads/$sequence_file_base.$i.fa | perl -nae 's/\n//; print' | perl -nae 's/(.{60})/$1\n/g; print'
		echo
	else
		tail -n+2 downloads/$sequence_file_base.$i.fa
	fi
done > $fasta_dir/$fasta_file

# Index
echo "Indexing FASTA file..."
samtools/samtools faidx $fasta_dir/$fasta_file

# Dictionary
echo "Creating FASTA dictionary..."
java -Xmx1g -jar $picard_jar R= $fasta_dir/$fasta_file O= $fasta_dir/$fasta_dict_file

# Archive
echo "Archiving FASTA / FAI files..."
cd $fasta_dir
echo -e "FASTA / FAI created by http://goo.gl/p9QvyN and can be downloaded by:\n\n   curl -g '$target' | tar zvx" > $fasta_readme
tar zcvf $archive_file $fasta_file $fasta_fai_file $fasta_dict_file $fasta_readme
cd -

# Deploy
source=$fasta_dir/$archive_file
target=$artifact_server/$artifact_repository/$artifact_path/$artifact_version/$archive_file

echo "Deploying $source to $target"
curl -v --user $username:$password --upload-file $source $target
