#!/bin/bash -e
# DCC-1885
# usage: ./migration.sh /path/to/input/dir /path/to/dictionary.json /path/to/output/dir
# TODO: pythonify this script as well (must read dictionary)
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

mkdir -p ${output_dir?}

# TODO: loop
if [ -f ${input_dir?}/*donor* ]; then
 # TODO: print name
 stream ${input_dir?}/*donor*    | ./migration.py ${dictionary_file?} donor "test" > ${output_dir?}/donor.txt
fi
if [ -f ${input_dir?}/*specimen* ]; then
 stream ${input_dir?}/*specimen* | ./migration.py ${dictionary_file?} specimen "test" > ${output_dir?}/specimen.txt
fi
if [ -f ${input_dir?}/*sample* ]; then
 stream ${input_dir?}/*sample*   | ./migration.py ${dictionary_file?} sample "test" > ${output_dir?}/sample.txt
fi

if [ -f ${input_dir?}/ssm*__p* ]; then
 echo ${input_dir?}/ssm*__p*
 stream ${input_dir?}/ssm*__p*   | ./migration.py ${dictionary_file?} ssm_p "test" > ${output_dir?}/ssm__p.txt
fi
if [ -f ${input_dir?}/ssm*__m* ]; then
 stream ${input_dir?}/ssm*__m*   | ./migration.py ${dictionary_file?} ssm_m "test" > ${output_dir?}/ssm__m.txt
fi

if [ -f ${input_dir?}/sgv*__m* ]; then
 stream ${input_dir?}/sgv*__m*    | ./migration.py ${dictionary_file?} sgv_m "test" > ${output_dir?}/sgv__m.txt
fi
if [ -f ${input_dir?}/sgv*__p* ]; then
 stream ${input_dir?}/sgv*__p*    | ./migration.py ${dictionary_file?} sgv_p "test" > ${output_dir?}/sgv__p.txt
fi

if [ -f ${input_dir?}/cnsm*__m* ]; then
 stream ${input_dir?}/cnsm*__m*    | ./migration.py ${dictionary_file?} cnsm_m "test" > ${output_dir?}/cnsm__m.txt
fi
if [ -f ${input_dir?}/cnsm*__p* ]; then
 stream ${input_dir?}/cnsm*__p*    | ./migration.py ${dictionary_file?} cnsm_p "test" > ${output_dir?}/cnsm__p.txt
fi
if [ -f ${input_dir?}/cnsm*__s* ]; then
 stream ${input_dir?}/cnsm*__s*    | ./migration.py ${dictionary_file?} cnsm_s "test" > ${output_dir?}/cnsm__s.txt
fi

if [ -f ${input_dir?}/stsm*__m* ]; then
 stream ${input_dir?}/stsm*__m*    | ./migration.py ${dictionary_file?} stsm_m "test" > ${output_dir?}/stsm__m.txt
fi
if [ -f ${input_dir?}/stsm*__p* ]; then
 stream ${input_dir?}/stsm*__p*    | ./migration.py ${dictionary_file?} stsm_p "test" > ${output_dir?}/stsm__p.txt
fi
if [ -f ${input_dir?}/stsm*__s* ]; then
 stream ${input_dir?}/stsm*__s*    | ./migration.py ${dictionary_file?} stsm_s "test" > ${output_dir?}/stsm__s.txt
fi

if [ -f ${input_dir?}/meth*__m* ]; then
 stream ${input_dir?}/meth*__m*    | ./migration.py ${dictionary_file?} meth_m "test" > ${output_dir?}/meth__m.txt
fi
if [ -f ${input_dir?}/meth*__p* ]; then
 stream ${input_dir?}/meth*__p*    | ./migration.py ${dictionary_file?} meth_p "test" > ${output_dir?}/meth__p.txt
fi
if [ -f ${input_dir?}/meth*__s* ]; then
 stream ${input_dir?}/meth*__s*    | ./migration.py ${dictionary_file?} meth_s "test" > ${output_dir?}/meth__s.txt
fi

if [ -f ${input_dir?}/mirna*__m* ]; then
 stream ${input_dir?}/mirna*__m*    | ./migration.py ${dictionary_file?} mirna_m "test" > ${output_dir?}/mirna__m.txt
fi
if [ -f ${input_dir?}/mirna*__p* ]; then
 stream ${input_dir?}/mirna*__p*    | ./migration.py ${dictionary_file?} mirna_p "test" > ${output_dir?}/mirna__p.txt
fi
if [ -f ${input_dir?}/mirna*__s* ]; then
 stream ${input_dir?}/mirna*__s*    | ./migration.py ${dictionary_file?} mirna_s "test" > ${output_dir?}/mirna__s.txt
fi

if [ -f ${input_dir?}/exp*__g* ]; then
 stream ${input_dir?}/exp*__g*    | ./migration.py ${dictionary_file?} exp_g "test" > ${output_dir?}/exp__g.txt
fi
if [ -f ${input_dir?}/exp*__m* ]; then
 stream ${input_dir?}/exp*__m*    | ./migration.py ${dictionary_file?} exp_m "test" > ${output_dir?}/exp__m.txt
fi

if [ -f ${input_dir?}/pexp*__m* ]; then
 stream ${input_dir?}/pexp*__m*    | ./migration.py ${dictionary_file?} pexp_m "test" > ${output_dir?}/pexp__m.txt
fi
if [ -f ${input_dir?}/pexp*__p* ]; then
 stream ${input_dir?}/pexp*__p*    | ./migration.py ${dictionary_file?} pexp_p "test" > ${output_dir?}/pexp__p.txt
fi

if [ -f ${input_dir?}/jcn*__m* ]; then
 stream ${input_dir?}/jcn*__m*    | ./migration.py ${dictionary_file?} jcn_m "test" > ${output_dir?}/jcn__m.txt
fi
if [ -f ${input_dir?}/jcn*__p* ]; then
 stream ${input_dir?}/jcn*__p*    | ./migration.py ${dictionary_file?} jcn_p "test" > ${output_dir?}/jcn__p.txt
fi

# ===========================================================================

