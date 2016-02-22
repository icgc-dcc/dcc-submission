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

--
-- Check if col_value is does not represent a missing value
--
CREATE OR REPLACE FUNCTION has_value(col_value text) RETURNS boolean as $func$
BEGIN
	IF (col_value IS NULL OR col_value IN ('-777','-888','-999','-9999') ) THEN
		RETURN FALSE;
	ELSE
		RETURN TRUE;
	END IF;
END;
$func$ LANGUAGE plpgsql;

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
-- Report table completeness
--
CREATE OR REPLACE FUNCTION table_completeness(_proj text, _schema text, _tbl text) RETURNS TABLE (
	column_name text,
	completeness col_coverage
	) as $$
DECLARE
	col_name text;
	compl col_coverage;
BEGIN
	FOR col_name IN EXECUTE format('SELECT column_name FROM information_schema.columns WHERE table_schema = %L AND table_name = %L', _schema, _tbl) LOOP
		SELECT * FROM col_proj_complete(_proj, col_name, format('%s.%s', _schema, _tbl)::regclass ) INTO compl;
		RETURN QUERY SELECT col_name,compl; 
	END LOOP;
END;
$$ LANGUAGE plpgsql;

---
--- Report release completeness
---
CREATE OR REPLACE FUNCTION release_completeness(release text, _tbl text) RETURNS TABLE (
	column_name text,
	completeness col_coverage
	) as $$
DECLARE
	proj text;
BEGIN
	FOR proj IN EXECUTE format('SELECT project_id FROM %s.project', release) LOOP
		RETURN QUERY EXECUTE format('SELECT * FROM table_completeness(%L, %L, %L)', proj, release, _tbl);
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
	project_id 		varchar,
	donor_id		varchar,
	sequencing_strategy	varchar
	) as $$
DECLARE
	tbl_name text;
BEGIN
	FOR tbl_name IN EXECUTE format('SELECT table_name FROM information_schema.tables WHERE table_name like ''%%_m'' AND table_schema = %L', release) LOOP
		IF (is_seq_str_tbl(tbl_name)) THEN
			RETURN QUERY EXECUTE format('SELECT project_id, donor_id, sequencing_strategy FROM %I.%I ', release, tbl_name);
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
	donor_id 	varchar,
	project_id	varchar,
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

--
-- Get table names of all meta file types in the release.
--
CREATE OR REPLACE FUNCTION get_meta_tables(_release text) RETURNS TABLE (table_name text) as $$
BEGIN
	RETURN QUERY EXECUTE format('SELECT CAST(table_name as text) FROM information_schema.tables WHERE table_name like ''%%_m'' AND table_schema = %L', _release);
END;
$$ LANGUAGE plpgsql;

