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
package org.icgc.dcc.submission.loader.db.orientdb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.submission.loader.db.orientdb.OrientdbDatabseService.exists;
import static org.icgc.dcc.submission.loader.util.DatabaseFields.PROJECT_ID_FIELD_NAME;
import static org.icgc.dcc.submission.loader.util.Services.createSubmissionService;
import static org.icgc.dcc.submission.loader.util.Strings.capitalize;
import lombok.val;

import org.icgc.dcc.submission.loader.meta.SubmissionMetadataService;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

@Ignore("Should consider dropping OrientDB support")
public class OrientdbDatabseServiceTest {

  private static final String DB_URL = "memory:test";

  ODatabaseDocumentTx db;
  OrientdbDatabseService dbService;
  SubmissionMetadataService submissionService;

  @Before
  @SuppressWarnings("resource")
  public void setUp() {
    this.db = new ODatabaseDocumentTx(DB_URL).create();
    this.submissionService = createSubmissionService();
    this.dbService = new OrientdbDatabseService(db, submissionService);
  }

  @After
  public void tearDown() {
    db.drop();
  }

  @Test
  public void testInitializeSchema() throws Exception {
    dbService.initializeSchema();

    // Verification
    for (val typeDef : submissionService.getFileTypes()) {
      val schemaName = capitalize(typeDef.getType());
      assertThat(exists(db, schemaName)).isTrue();
    }

    val specimenShcema = db.getMetadata().getSchema().getClass("Specimen");
    assertThat(specimenShcema.existsProperty("sample")).isTrue();
    assertThat(specimenShcema.existsProperty("donor")).isTrue();
  }

  @Test
  public void testCreateTypeIndex() throws Exception {
    val donorSchema = db.getMetadata().getSchema().createClass("Donor");
    donorSchema.createProperty("donor_id", OType.STRING);
    donorSchema.createProperty(PROJECT_ID_FIELD_NAME, OType.STRING);

    dbService.createTypeIndex("donor");
    val sql = "SELECT COUNT(*) FROM INDEX:donor_idx";
    // If the index does not exist an exception will be thrown
    db.query(new OSQLSynchQuery<ODocument>(sql));
    // Test if duplicate index will not be thrown
    dbService.createTypeIndex("donor");
  }

}
