-- Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.
--
-- This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
-- You should have received a copy of the GNU General Public License along with
-- this program. If not, see <http://www.gnu.org/licenses/>.
--
-- THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
-- EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
-- OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
-- SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
-- INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
-- TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
-- OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
-- IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
-- ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

----------------------------------------------------------------------------------------------------------------
-- Type definitions
----------------------------------------------------------------------------------------------------------------

--
-- Column completeness data type
--
DROP TYPE IF EXISTS col_coverage CASCADE;
CREATE TYPE col_coverage AS (
	project_id	text,
	total 		numeric,
	with_data	numeric,
	coverage	numeric
);

----------------------------------------------------------------------------------------------------------------
-- Helper functions
----------------------------------------------------------------------------------------------------------------

--
-- Check if col_value is does not represent a missing value
--
CREATE OR REPLACE FUNCTION has_value(col_value text) RETURNS boolean as $func$
BEGIN
	IF (col_value IS NULL OR col_value IN ('-777','-888','-999','-9999','N/A','none') ) THEN
		RETURN FALSE;
	ELSE
		RETURN TRUE;
	END IF;
END;
$func$ LANGUAGE plpgsql;

--
-- Get table names of all file types in the release.
--
CREATE OR REPLACE FUNCTION get_table_names(_release text) RETURNS TABLE (table_name text) as $$
BEGIN
	RETURN QUERY EXECUTE format('
		SELECT 
			CAST(table_name as text) 
		FROM 
			information_schema.tables 
		WHERE 
			table_name != ''completeness'' AND table_schema = %L', _release);
END;
$$ LANGUAGE plpgsql;

--
-- Get table names of all meta file types in the release.
--
CREATE OR REPLACE FUNCTION get_meta_tables(_release text) RETURNS TABLE (table_name text) as $$
BEGIN
	RETURN QUERY EXECUTE format('SELECT CAST(table_name as text) FROM information_schema.tables WHERE table_name like ''%%_m'' AND table_schema = %L', _release);
END;
$$ LANGUAGE plpgsql;

--
-- Get column names for a _table in _schema
--
CREATE OR REPLACE FUNCTION get_column_names(_schema text, _table text) RETURNS TABLE (column_name text) as $$
BEGIN
	RETURN QUERY EXECUTE format('SELECT CAST(column_name as text) FROM information_schema.columns WHERE table_schema = %L AND table_name = %L', _schema, _table);
END;
$$ LANGUAGE plpgsql;

----------------------------------------------------------------------------------------------------------------
-- Completeness calculation helper functions
----------------------------------------------------------------------------------------------------------------

--
--
--
CREATE OR REPLACE FUNCTION calc_dependent_completeness(_proj text, _tbl regclass, _child_col text, _parent_col text, _parent_cond text[]) RETURNS col_coverage as $$
DECLARE
	total numeric;
	with_data numeric;
	completeness numeric := 0;
BEGIN
	EXECUTE 
		format('SELECT * FROM dependent_expected_count(%L, %L, %L, %L)',
			_proj, _tbl, _parent_col, _parent_cond)
	INTO total;

	EXECUTE 
		format('SELECT * FROM dependent_completed_count(%L, %L, %L, %L, %L)',
			_proj, _tbl, _child_col, _parent_col, _parent_cond)
	INTO with_data;

	IF (total != 0) THEN
		completeness := with_data / total * 100;
	END IF;

	RETURN ROW(_proj, total, with_data, completeness);
END;
$$ LANGUAGE plpgsql;

--
--
--
CREATE OR REPLACE FUNCTION dependent_expected_count(_proj text, _tbl regclass, _parent_col text, _parent_cond text[]) RETURNS integer as $$
DECLARE
	result integer;
BEGIN
	EXECUTE format('SELECT count(*) FROM %s WHERE project_id = %L AND (%s = ANY(%L) OR NOT has_value(%I))',
		_tbl, _proj, _parent_col, _parent_cond, _parent_col)
	INTO result;

	RETURN result;
END;
$$ LANGUAGE plpgsql;

--
-- Calculates number of records completed as required because of parent's value
-- _tbl - source table for completeness calculation
-- _child_col - column name to be completed because of parent's value
-- _parent_col - parent's column name which value defined that child column should be completed
-- _parent_cond - on what conditions the _child_col much be filled in
--
-- E.g. 
--	SELECT 
--		count(*)
--	FROM
--		icgc20.donor
--	WHERE
--		( donor_vital_status = 'deceased' OR NOT has_value(donor_vital_status) )
--		AND has_value(donor_survival_time)
CREATE OR REPLACE FUNCTION dependent_completed_count(_proj text, _tbl regclass, _child_col text, _parent_col text, _parent_cond text[]) RETURNS integer as $$
DECLARE
	result integer;
BEGIN
	EXECUTE format('SELECT count(*) FROM %s 
		WHERE 
			project_id = %L AND has_value(%I) AND (%s = ANY(%L) OR NOT has_value(%I))', 
		_tbl, _proj, _child_col, _parent_col, _parent_cond, _parent_col)
	INTO result;

	RETURN result;

END;
$$ LANGUAGE plpgsql;

--
-- Report column completeness
--
CREATE OR REPLACE FUNCTION col_proj_complete(_proj text, col text, _tbl regclass) RETURNS col_coverage as $$
DECLARE
        total numeric;
        w_data numeric;
        completeness numeric := 0;
BEGIN
        EXECUTE format('SELECT count(*) FROM %s WHERE project_id = $1', _tbl)
        INTO total
        USING _proj;

        EXECUTE format('SELECT count(%I) FROM %s WHERE project_id = $1 AND has_value(%I) ', col, _tbl, col)
        INTO w_data
        USING _proj;

	IF (total != 0) THEN
        	completeness := w_data / total * 100;
	END IF;
        RETURN ROW(_proj, total, w_data, completeness);
END;
$$ LANGUAGE plpgsql;

--
-- Report column completeness
--
CREATE OR REPLACE FUNCTION column_completeness_with_rule(_proj text, col text, _tbl regclass, rules text[]) 
	RETURNS col_coverage as $$
DECLARE
        total numeric;
        w_data numeric;
        completeness numeric := 0;
BEGIN
        EXECUTE format('SELECT count(*) FROM %s WHERE project_id = $1', _tbl)
        INTO total
        USING _proj;

        EXECUTE format('SELECT count(%I) FROM %s WHERE project_id = $1 AND %I = ANY(%L)', col, _tbl, col, rules)
        INTO w_data
        USING _proj;

	IF (total != 0) THEN
        	completeness := w_data / total * 100;
	END IF;
        RETURN ROW(_proj, total, w_data, completeness);
END;
$$ LANGUAGE plpgsql;

--
--
--
CREATE OR REPLACE FUNCTION calc_celluarity_completeness(_proj text, _tbl regclass) RETURNS col_coverage as $$
DECLARE
        total numeric;
        w_data numeric;
        completeness numeric := 0;
BEGIN
        EXECUTE format('SELECT count(*) FROM %s WHERE project_id = $1', _tbl)
        INTO total
        USING _proj;

        EXECUTE format('SELECT count(*) FROM %s 
        	WHERE 
        		project_id = $1 AND (has_value(percentage_cellularity) OR has_value(level_of_cellularity))', _tbl)
        INTO w_data
        USING _proj;

	IF (total != 0) THEN
        	completeness := w_data / total * 100;
	END IF;
        RETURN ROW(_proj, total, w_data, completeness);
END;
$$ LANGUAGE plpgsql;

----------------------------------------------------------------------------------------------------------------
-- File specific completeness calculation functions
----------------------------------------------------------------------------------------------------------------

--
-- Donor completeness calculation function
--
CREATE OR REPLACE FUNCTION donor_completeness(_proj text, _schema text) RETURNS TABLE (
	column_name text,
	project_id	text,
	total 		numeric,
	with_data	numeric,
	coverage	numeric
	) as $$
DECLARE
	col_name text;
	compl col_coverage;
BEGIN
	FOR col_name IN EXECUTE format('SELECT * FROM get_column_names(%L, %L)', _schema, 'donor') LOOP

		IF (col_name = 'donor_survival_time') THEN
			EXECUTE format('SELECT * FROM calc_dependent_completeness(%L, ''%s.donor''::regclass, %L, %L, %L)', 
				_proj, _schema, col_name, 'donor_vital_status', ARRAY['deceased'])
			INTO compl;

		ELSIF (col_name = 'donor_relapse_interval') THEN
			EXECUTE format('SELECT * FROM calc_dependent_completeness(%L, ''%s.donor''::regclass, %L, %L, %L)', 
				_proj, _schema, col_name, 'disease_status_last_followup', ARRAY['progression', 'relapse'])
			INTO compl;

		ELSIF (col_name = 'donor_interval_of_last_followup') THEN
			EXECUTE format('SELECT * FROM calc_dependent_completeness(%L, ''%s.donor''::regclass, %L, %L, %L)', 
				_proj, _schema, col_name, 'disease_status_last_followup', ARRAY['progression', 'relapse'])
			INTO compl;

		ELSIF (col_name = 'cancer_history_first_degree_relative') THEN
			EXECUTE format('SELECT * FROM column_completeness_with_rule(%L, %L, ''%s.donor''::regclass, %L)', 
				_proj, col_name, _schema, ARRAY['yes', 'no'])
			INTO compl;
		
		ELSIF (col_name = 'prior_malignancy') THEN
			EXECUTE format('SELECT * FROM column_completeness_with_rule(%L, %L, ''%s.donor''::regclass, %L)', 
				_proj, col_name, _schema, ARRAY['yes', 'no'])
			INTO compl;

		ELSE
			EXECUTE format('SELECT * FROM col_proj_complete(%L, %L, ''%s.donor''::regclass)', 
				_proj, col_name,_schema)
			INTO compl; 
		END IF;

		 RETURN QUERY SELECT col_name,(compl).project_id, (compl).total, (compl).with_data, (compl).coverage; 
	END LOOP;
END;
$$ LANGUAGE plpgsql;

--
-- Specimen completeness calculation function
--
CREATE OR REPLACE FUNCTION specimen_completeness(_proj text, _schema text) RETURNS TABLE (
	column_name text,
	project_id	text,
	total 		numeric,
	with_data	numeric,
	coverage	numeric
	) as $$
DECLARE
	col_name text;
	compl col_coverage;
BEGIN
	FOR col_name IN EXECUTE format('SELECT * FROM get_column_names(%L, %L)', _schema, 'specimen') LOOP

		IF (col_name ~ '^tumour_|specimen_donor_treatment_type') THEN
			EXECUTE format('SELECT * FROM calc_dependent_completeness(%L, ''%s.specimen''::regclass, %L, %L, %L)', 
				_proj, _schema, col_name, 'specimen_type', 
				ARRAY['Primary tumour - solid tissue', 'Primary tumour - blood derived (peripheral blood)',
				'Primary tumour - blood derived (bone marrow)', 'Primary tumour - additional new primary',
				'Primary tumour - other', 'Recurrent tumour - solid tissue', 'Recurrent tumour - blood derived (peripheral blood)',
				'Recurrent tumour - blood derived (bone marrow)', 'Recurrent tumour - other', 'Metastatic tumour - NOS',
				'Metastatic tumour - lymph node', 'Metastatic tumour - metastatsis local to lymph node',
				'Metastatic tumour - metastatsis to distant location', 'Metastatic tumour - additional metastatic', 
				'Xenograft - derived from primary tumour', 'Xenograft - derived from tumour cell line',
				'Cell line - derived from tumour', 'Primary tumour - lymph node'])
			INTO compl;

		ELSIF (col_name ~ '_cellularity') THEN
			EXECUTE format('SELECT * FROM calc_celluarity_completeness(%L, ''%s.specimen''::regclass)', _proj, _schema)
			INTO compl;

		ELSE
			EXECUTE format('SELECT * FROM col_proj_complete(%L, %L, ''%s.specimen''::regclass)', _proj, col_name,_schema)
			INTO compl; 
		END IF;

		RETURN QUERY SELECT col_name,(compl).project_id, (compl).total, (compl).with_data, (compl).coverage; 
	END LOOP;
END;
$$ LANGUAGE plpgsql;

--
-- Sample completeness calculation function
--
CREATE OR REPLACE FUNCTION sample_completeness(_proj text, _schema text) RETURNS TABLE (
	column_name text,
	project_id	text,
	total 		numeric,
	with_data	numeric,
	coverage	numeric
	) as $$
DECLARE
	col_name text;
	compl col_coverage;
BEGIN
	FOR col_name IN EXECUTE format('SELECT * FROM get_column_names(%L, %L)', _schema, 'sample') LOOP

		IF (col_name ~ '_cellularity') THEN
			EXECUTE format('SELECT * FROM calc_celluarity_completeness(%L, ''%s.sample''::regclass)', _proj, _schema)
			INTO compl;

		ELSE
			EXECUTE format('SELECT * FROM col_proj_complete(%L, %L, ''%s.sample''::regclass)', _proj, col_name,_schema)
			INTO compl; 
		END IF;

		RETURN QUERY SELECT col_name,(compl).project_id, (compl).total, (compl).with_data, (compl).coverage; 
	END LOOP;
END;
$$ LANGUAGE plpgsql;

--
-- Exposure completeness calculation function
--
CREATE OR REPLACE FUNCTION exposure_completeness(_proj text, _schema text) RETURNS TABLE (
	column_name text,
	project_id	text,
	total 		numeric,
	with_data	numeric,
	coverage	numeric
	) as $$
DECLARE
	col_name text;
	compl col_coverage;
BEGIN
	FOR col_name IN EXECUTE format('SELECT * FROM get_column_names(%L, %L)', _schema, 'exposure') LOOP

		IF (col_name = 'tobacco_smoking_intensity') THEN
			EXECUTE format('SELECT * FROM calc_dependent_completeness(%L, ''%s.exposure''::regclass, %L, %L, %L)', 
				_proj, _schema, col_name, 'tobacco_smoking_history_indicator', 
				ARRAY['Current smoker (includes daily smokers non-daily/occasional smokers)',
				'Current reformed smoker for > 15 years',
				'Current reformed smoker for <= 15 years',
				'Current reformed smoker, duration not specified'])
			INTO compl;

		ELSIF (col_name = 'alcohol_history_intensity') THEN
			EXECUTE format('SELECT * FROM calc_dependent_completeness(%L, ''%s.exposure''::regclass, %L, %L, %L)', 
				_proj, _schema, col_name, 'alcohol_history', ARRAY['yes'])
			INTO compl;

		ELSE
			EXECUTE format('SELECT * FROM col_proj_complete(%L, %L, ''%s.exposure''::regclass)', 
				_proj, col_name,_schema)
		 INTO compl; 
		END IF;

		RETURN QUERY SELECT col_name,(compl).project_id, (compl).total, (compl).with_data, (compl).coverage; 
	END LOOP;
END;
$$ LANGUAGE plpgsql;

--
-- Family completeness calculation function
--
CREATE OR REPLACE FUNCTION family_completeness(_proj text, _schema text) RETURNS TABLE (
	column_name text,
	project_id	text,
	total 		numeric,
	with_data	numeric,
	coverage	numeric
	) as $$
DECLARE
	col_name text;
	compl col_coverage;
BEGIN
	FOR col_name IN EXECUTE format('SELECT * FROM get_column_names(%L, %L)', _schema, 'family') LOOP

		IF (col_name ~ 'relationship_sex|relationship_age|relationship_disease_icd10|relationship_disease') THEN
			EXECUTE format('SELECT * FROM calc_dependent_completeness(%L, ''%s.family''::regclass, %L, %L, %L)', 
				_proj, _schema, col_name, 'relationship_type', 
				ARRAY['sibling','parent','grandparent', 'uncle/aunt','cousin'])
			INTO compl;

		ELSIF (col_name = 'relationship_type') THEN
			EXECUTE format('SELECT * FROM calc_dependent_completeness(%L, ''%s.family''::regclass, %L, %L, %L)', 
				_proj, _schema, col_name, 'donor_has_relative_with_cancer_history', ARRAY['yes'])
			INTO compl;

		ELSE
			EXECUTE format('SELECT * FROM col_proj_complete(%L, %L, ''%s.family''::regclass)', _proj, col_name,_schema)
			INTO compl; 
		END IF;

		RETURN QUERY SELECT col_name,(compl).project_id, (compl).total, (compl).with_data, (compl).coverage; 
	END LOOP;
END;
$$ LANGUAGE plpgsql;

--
-- Therapy completeness calculation function
--
CREATE OR REPLACE FUNCTION therapy_completeness(_proj text, _schema text) RETURNS TABLE (
	column_name text,
	project_id	text,
	total 		numeric,
	with_data	numeric,
	coverage	numeric
	) as $$
DECLARE
	col_name text;
	compl col_coverage;
BEGIN
	FOR col_name IN EXECUTE format('SELECT * FROM get_column_names(%L, %L)', _schema, 'therapy') LOOP

		IF (col_name ~ 'first_therapy_response|first_therapy_start_interval|first_therapy_duration') THEN
			EXECUTE format('SELECT * FROM calc_dependent_completeness(%L, ''%s.therapy''::regclass, %L, %L, %L)', 
				_proj, _schema, col_name, 'first_therapy_type', 
				ARRAY['chemotherapy','radiation therapy','combined chemo+radiation therapy', 'immunotherapy','combined chemo+immunotherapy',
				'surgery', 'other therapy', 'bone marrow transplant', 'stem cell transplant', 'monoclonal antibodies (for liquid tumours)'])
			INTO compl;

		ELSIF (col_name ~ 'second_therapy_response|second_therapy_start_interval|second_therapy_duration') THEN
			EXECUTE format('SELECT * FROM calc_dependent_completeness(%L, ''%s.therapy''::regclass, %L, %L, %L)', 
				_proj, _schema, col_name, 'second_therapy_type', 
				ARRAY['chemotherapy','radiation therapy','combined chemo+radiation therapy', 'immunotherapy','combined chemo+immunotherapy',
				'surgery', 'other therapy', 'bone marrow transplant', 'stem cell transplant', 'monoclonal antibodies (for liquid tumours)'])
			INTO compl;

		ELSE
			EXECUTE format('SELECT * FROM col_proj_complete(%L, %L, ''%s.therapy''::regclass)', _proj, col_name,_schema)
			INTO compl; 
		END IF;

		RETURN QUERY SELECT col_name,(compl).project_id, (compl).total, (compl).with_data, (compl).coverage; 
	END LOOP;
END;
$$ LANGUAGE plpgsql;


----------------------------------------------------------------------------------------------------------------
-- Completeness calculation functions
----------------------------------------------------------------------------------------------------------------

--
-- Report table completeness
--
CREATE OR REPLACE FUNCTION table_completeness(_proj text, _schema text, _tbl text) RETURNS TABLE (
	column_name text,
	project_id	text,
	total 		numeric,
	with_data	numeric,
	coverage	numeric
	) as $$
DECLARE
	col_name text;
	compl col_coverage;
BEGIN
	FOR col_name IN EXECUTE format('SELECT column_name FROM information_schema.columns WHERE table_schema = %L AND table_name = %L', _schema, _tbl) LOOP
		SELECT * FROM col_proj_complete(_proj, col_name, format('%s.%s', _schema, _tbl)::regclass ) INTO compl;

		RETURN QUERY SELECT col_name,(compl).project_id, (compl).total, (compl).with_data, (compl).coverage; 
	END LOOP;
END;
$$ LANGUAGE plpgsql;

---
--- Report release completeness
---
CREATE OR REPLACE FUNCTION release_completeness(_release text) RETURNS TABLE (
	table_name text,
	column_name text,
	project_id	text,
	total 		numeric,
	with_data	numeric,
	coverage	numeric
	) as $$

DECLARE
	tbl text;

BEGIN
	FOR tbl IN EXECUTE format('SELECT * FROM get_table_names(%L)', _release) LOOP
		RETURN QUERY EXECUTE format('SELECT CAST(%L as text), rtc.* FROM release_table_completeness(%L, %L) as rtc',tbl, _release, tbl);
	END LOOP;
END;
$$ LANGUAGE plpgsql;

---
--- Report table completeness in the release
---
CREATE OR REPLACE FUNCTION release_table_completeness(release text, _tbl text) RETURNS TABLE (
	column_name text,
	project_id	text,
	total 		numeric,
	with_data	numeric,
	coverage	numeric
	) as $$

DECLARE
	proj text;

BEGIN
	FOR proj IN EXECUTE format('SELECT project_id FROM %s.project', release) LOOP

		IF (_tbl = 'donor') THEN
			RETURN QUERY EXECUTE format('SELECT * FROM donor_completeness(%L, %L)', proj, release);

		ELSIF (_tbl = 'specimen') THEN
			RETURN QUERY EXECUTE format('SELECT * FROM specimen_completeness(%L, %L)', proj, release);

		ELSIF (_tbl = 'sample') THEN
			RETURN QUERY EXECUTE format('SELECT * FROM sample_completeness(%L, %L)', proj, release);

		ELSIF (_tbl = 'family') THEN
			RETURN QUERY EXECUTE format('SELECT * FROM family_completeness(%L, %L)', proj, release);

		ELSIF (_tbl = 'exposure') THEN
			RETURN QUERY EXECUTE format('SELECT * FROM exposure_completeness(%L, %L)', proj, release);

		ELSIF (_tbl = 'therapy') THEN
			RETURN QUERY EXECUTE format('SELECT * FROM therapy_completeness(%L, %L)', proj, release);

		ELSE
			RETURN QUERY EXECUTE format('SELECT * FROM table_completeness(%L, %L, %L)', proj, release, _tbl);
		END IF;

	END LOOP;
END;
$$ LANGUAGE plpgsql;

--
-- Check if the table name '_tbl' contains the sequencing_stragety column.
--
CREATE OR REPLACE FUNCTION is_seq_str_tbl(_tbl text) RETURNS boolean as $$
BEGIN
        IF (_tbl IN ('exp_array_m', 'meth_array_m', 'pexp_m') ) THEN
                RETURN FALSE;
        ELSE
                RETURN TRUE;
        END IF;
	
END;
$$ LANGUAGE plpgsql;

--
-- Get sequencing stragety
--
CREATE OR REPLACE FUNCTION get_seq_str(release text) RETURNS TABLE (
	project_id varchar,
	donor_id varchar,
	file_type	text,
	sequencing_strategy	varchar
	) as $$
DECLARE
	tbl_name text;
BEGIN
	FOR tbl_name IN EXECUTE format('SELECT table_name FROM information_schema.tables WHERE table_name like ''%%_m'' AND table_schema = %L', release) LOOP
		IF (is_seq_str_tbl(tbl_name)) THEN
			RETURN QUERY EXECUTE 
				format('
					SELECT 
						project_id, donor_id, SUBSTRING(CAST(%L as text), ''(.*)_m$'') AS file_type, sequencing_strategy 
					FROM %I.%I ', tbl_name, release, tbl_name);
		END IF;
	END LOOP;
END;
$$ LANGUAGE plpgsql;

--
-- Get file type records for donor. Value returned in format
-- +------------|----------|-----------+
-- | project_id | donor_id | file_type |
-- +-----------------------------------+
-- | TST-CA     | DO1      | ssm       |
-- +-----------------------------------+
--
CREATE OR REPLACE FUNCTION get_donor_file_type(_release text) RETURNS TABLE (
	project_id 	varchar,
	donor_id	varchar,
	file_type	text
	) as $$
DECLARE
	tbl_name text;
BEGIN
	FOR tbl_name IN EXECUTE format('SELECT * FROM get_meta_tables(%L)', _release) LOOP
		RETURN QUERY EXECUTE format('SELECT project_id, donor_id, SUBSTRING(CAST(%L as text), ''(.*)_m$'') AS file_type FROM %I.%I', tbl_name, _release, tbl_name);
	END LOOP;
END;
$$ LANGUAGE plpgsql;

----------------------------------------------------------------------------------------------------------------
-- Reporting functions
----------------------------------------------------------------------------------------------------------------

CREATE OR REPLACE FUNCTION get_file_type_stats(_release text) RETURNS TABLE (
	project_id 	varchar,
	file_type	text,
	total_clinical_donors	bigint,
	orphan_donors	bigint,
	donors_per_file_type	bigint
	) as $$

BEGIN
	RETURN QUERY EXECUTE format(
		'WITH 
			total_donors AS (
			SELECT 
				project_id, count(donor_id) total 
			FROM 
				%I.donor 
			GROUP BY 
				project_id),
			orphan_donor AS (
			SELECT 
				project_id, count(distinct(donor_id)) donors_with_data 
			FROM 
				get_donor_file_type(%L) 
			GROUP BY 
				project_id)
		SELECT 
			f.project_id, 
			f.file_type, 
			total_donors.total as total_clinical_donors, 
			total_donors.total - orphan_donor.donors_with_data as orphan_donors, 
			count(distinct(f.donor_id)) donors_per_file_type 
		FROM 
			get_donor_file_type(%L) f, total_donors, orphan_donor
		WHERE 
			f.project_id = total_donors.project_id 
			AND f.project_id = orphan_donor.project_id
		GROUP BY 
			f.project_id, f.file_type, total_donors.total, orphan_donors
		ORDER BY 
			f.project_id, f.file_type',
		_release, _release, _release);
END;
$$ LANGUAGE plpgsql;
