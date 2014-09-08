#!/bin/bash -e

scripts="/mnt/proxyprod/git/branches/migration_testing/src/main/scripts/DCC-1925"
${scripts?}/vep.py \
 /home/tony/git/git0/data-submission/dcc-submission/dcc-submission-validator/target/test-classes/fixtures/validation/external/error/fk_1/.validation \
 /tmp/DCC-1925

${scripts?}/intra_discarder.py \
 ./dcc-submission/dcc-submission-server/src/test/resources/fixtures/submission/submission/release1/project.6 \
 /tmp/DCC-1925

${scripts?}/inter_discarder.py \
 /tmp/DCC-1925

