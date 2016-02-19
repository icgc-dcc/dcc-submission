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

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.submission.loader.util.Fields.PROJECT_ID;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.loader.util.Services;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;

@Slf4j
@Ignore("Link documents is not implemented")
public class OrientdbDocumentLinkerTest {

  public static final String PROJECT_NAME = "ALL-US";

  OrientdbDocumentLinker documentLinker;
  ODatabaseDocumentTx db;

  @Before
  @SuppressWarnings("resource")
  public void setUp() {
    val submissionService = Services.createSubmissionService();
    this.db = new ODatabaseDocumentTx("memory:test").create();
    this.documentLinker = new OrientdbDocumentLinker(submissionService, db);
  }

  @After
  @SneakyThrows
  public void tearDown() {
    db.drop();
  }

  @Test
  public void testLinkDocuments() throws Exception {
    createSpecimenClass();
    loadDonorRecord();
    createSpecimenDoc().save();
    documentLinker.linkDocuments("specimen", singleton("donor"));

    assertDonor();
    assertSpecimen();
  }

  private void assertDonor() {
    val sql = "SELECT * FROM Donor";
    val results = db.query(new OSQLSynchQuery<ODocument>(sql));
    assertThat(results).hasSize(1);
    val donor = (ODocument) results.get(0);
    log.debug("Donor: {}", donor);

    val specimens = getFieldCollection(donor.field("specimen"));
    assertThat(specimens).hasSize(1);

    val specimen = specimens.iterator().next();
    assertThat(specimen.field("specimen_type").toString()).isEqualTo("some_type");
  }

  private void assertSpecimen() {
    val sql = "SELECT * FROM Specimen";
    val results = db.query(new OSQLSynchQuery<ODocument>(sql));
    assertThat(results).hasSize(1);
    val specimen = (ODocument) results.get(0);
    log.debug("Specimen: {}", specimen);

    val donors = getFieldCollection(specimen.field("donor"));
    assertThat(donors).hasSize(1);

    val donor = donors.iterator().next();
    assertThat(donor.field("donor_sex").toString()).isEqualTo("male");
  }

  @SuppressWarnings("unchecked")
  private static Collection<ODocument> getFieldCollection(Object field) {
    if (field instanceof Set) {
      return (Set<ODocument>) field;
    } else {
      return (List<ODocument>) field;
    }
  }

  private void createSpecimenClass() {
    val clazz = db.getMetadata().getSchema().createClass("Specimen");
    clazz.createProperty("donor_id", OType.STRING);
    clazz.createProperty("specimen_id", OType.STRING);
    clazz.createProperty("specimen_type", OType.STRING);
    clazz.createProperty(PROJECT_ID, OType.STRING);
  }

  private static ODocument createSpecimenDoc() {
    val specimenDoc = new ODocument("Specimen");
    specimenDoc.field("donor_id", "DO1");
    specimenDoc.field("specimen_id", "SP1");
    specimenDoc.field("specimen_type", "some_type");
    specimenDoc.field(PROJECT_ID, PROJECT_NAME);

    return specimenDoc;
  }

  private static void loadDonorRecord() {
    val donorDoc = new ODocument("Donor");
    donorDoc.field("donor_id", "DO1");
    donorDoc.field("donor_sex", "male");
    donorDoc.field(PROJECT_ID, PROJECT_NAME);

    donorDoc.save();
  }

}
