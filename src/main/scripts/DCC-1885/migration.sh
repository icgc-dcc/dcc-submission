#!/bin/bash -e
# DCC-1885
# usage: ./migration.sh /path/to/input/dir /path/to/dictionary.json /path/to/output/dir
# ===========================================================================

cd "$(dirname '$0')"
input_dir=${1?} && shift
dictionary_file=${1?} && shift
output_dir=${1?} && shift

# ===========================================================================

function stream() {
 input_file=${1?}
 input_filename=$(basename ${input_file?})
 if [[ "${input_filename?}" =~ \.txt$ ]]; then
  command="cat ${input_file?}"
 fi
 if [[ "${input_filename?}" =~ \.gz$ ]]; then
  command="gzip -cd ${input_file?}"
 fi
 if [[ "${input_filename?}" =~ \.bz2$ ]]; then
  command="bzip2 -cd ${input_file?}"
 fi
 eval "${command?}"
}

# ===========================================================================

if [ -f ${input_dir?}/*donor* ]; then
 stream ${input_dir?}/*donor*    | ./migration.py ${dictionary_file?} donor    > ${output_dir?}/donor.txt
fi
if [ -f ${input_dir?}/*specimen* ]; then
 stream ${input_dir?}/*specimen* | ./migration.py ${dictionary_file?} specimen > ${output_dir?}/specimen.txt
fi
if [ -f ${input_dir?}/*sample* ]; then
 stream ${input_dir?}/*sample*   | ./migration.py ${dictionary_file?} sample   > ${output_dir?}/sample.txt
fi
if [ -f ${input_dir?}/ssm*__p* ]; then
 stream ${input_dir?}/ssm*__p*   | ./migration.py ${dictionary_file?} ssm_p    > ${output_dir?}/ssm__p.txt
fi
if [ -f ${input_dir?}/ssm*__m* ]; then
 stream ${input_dir?}/ssm*__m*   | ./migration.py ${dictionary_file?} ssm_m    > ${output_dir?}/ssm__m.txt
fi

# ===========================================================================

