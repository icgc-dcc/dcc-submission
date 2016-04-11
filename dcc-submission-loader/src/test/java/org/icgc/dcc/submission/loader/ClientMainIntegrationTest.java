/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.loader;

import static org.assertj.core.api.Assertions.assertThat;

import org.icgc.dcc.submission.loader.util.AbstractPostgressTest;
import org.junit.Test;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

import lombok.Cleanup;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientMainIntegrationTest extends AbstractPostgressTest {

  private static final String SUBMISSION_URL = "";
  private static final String SUBMISSION_USER = "";
  private static final String SUBMISSION_PASSWORD = "";
  private static final String FIXTURES = "src/test/resources/fixtures/input";

  @Test
  @SuppressWarnings("resource")
  public void testOrientdbMain() throws Exception {
    val db = new ODatabaseDocumentTx("memory:test");
    db.create();

    ClientMain.main(
        "--hdfs-url", "file:///",
        "--db-host", "memory:test",
        "--db-port", "",
        "--db-name", "",
        "--db-user", "admin",
        "--db-password", "admin",
        "--submission-url", SUBMISSION_URL,
        "--submission-user", SUBMISSION_USER,
        "--submission-password", SUBMISSION_PASSWORD,
        "--db-type", "ORIENTDB",
        "--input-dir", FIXTURES);

    verifyDB();
    db.drop();
  }

  @Test
  public void testPostgresMain() throws Exception {
    ClientMain.main(
        "--hdfs-url", "file:///",
        "--db-host", config.net().host(),
        "--db-port", String.valueOf(config.net().port()),
        "--db-name", config.storage().dbName(),
        "--db-user", config.credentials().username(),
        "--db-password", config.credentials().password(),
        "--submission-url", SUBMISSION_URL,
        "--submission-user", SUBMISSION_USER,
        "--submission-password", SUBMISSION_PASSWORD,
        "--threads", "1",
        "--input-dir", FIXTURES);

    verifyPostgresDB();
  }

  private void verifyPostgresDB() {
    assertThat(jdbcTemplate.queryForObject("select count(*) from icgc20.donor", Integer.class)).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("select count(*) from icgc20.specimen", Integer.class)).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("select count(*) from icgc20.sample", Integer.class)).isEqualTo(1);
    assertThat(jdbcTemplate.queryForObject("select count(*) from icgc20.ssm_m", Integer.class)).isEqualTo(1);
    val ssm_ps = jdbcTemplate.queryForList("select * from icgc20.ssm_p");
    log.debug("SSM_P: {}", ssm_ps);
    assertThat(ssm_ps).hasSize(2);
    assertThat(ssm_ps.get(0).get("chromosome_start")).isEqualTo("5068360");
    assertThat(ssm_ps.get(0).get("biological_validation_status")).isEqualTo("not tested");
    assertThat(ssm_ps.get(1).get("chromosome_start")).isEqualTo("36872057");
    assertThat(jdbcTemplate.queryForObject("select count(*) from icgc20.completeness", Integer.class)).isEqualTo(479);
  }

  private static void verifyDB() {
    @Cleanup
    val db = new ODatabaseDocumentTx("memory:test");
    db.open("admin", "admin");

    // Traverse from parent to child
    val donors = db.query(new OSQLSynchQuery<ODocument>("select * from Donor"));
    assertThat(donors).hasSize(1);

    val specimens = db.query(new OSQLSynchQuery<ODocument>("select * from Specimen"));
    assertThat(specimens).hasSize(1);

    val samples = db.query(new OSQLSynchQuery<ODocument>("select * from Sample"));
    assertThat(samples).hasSize(1);

    val ssm_m = db.query(new OSQLSynchQuery<ODocument>("select * from Ssm_m"));
    assertThat(ssm_m).hasSize(1);

    val ssm_p = db.query(new OSQLSynchQuery<ODocument>("select * from Ssm_p"));
    assertThat(ssm_p).hasSize(2);
  }

}
