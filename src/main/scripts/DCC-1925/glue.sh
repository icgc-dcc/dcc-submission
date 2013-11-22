#!/bin/bash -e

/mnt/proxyprod/git/branches/migration_testing/src/main/scripts/DCC-1925/vep.py \
 /home/tony/git/git0/data-submission/dcc-submission/dcc-submission-validator/target/test-classes/fixtures/validation/external/error/fk_1/.validation \
 /tmp/DCC-1925

/mnt/proxyprod/git/branches/migration_testing/src/main/scripts/DCC-1925/discarder.py \
 ./dcc-submission/dcc-submission-server/src/test/resources/fixtures/submission/dcc_root_dir/release1/project.6 \
 /tmp/DCC-1925

