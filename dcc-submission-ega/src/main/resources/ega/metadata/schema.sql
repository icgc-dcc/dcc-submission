CREATE SCHEMA ega;

CREATE TABLE IF NOT EXISTS ega.ega_sample_mapping_${timestamp_in_second} (
  sample_id varchar(64),
  file_id varchar(64)
);

CREATE OR REPLACE VIEW ega.view_ega_sample_mapping AS SELECT * from ega.${table_name};

CREATE TABLE IF NOT EXISTS ega.bad_ega_sample_metadata (
  timestamp bigint,
  file_name varchar(64),
  line_number int,
  line_content varchar(256)
);