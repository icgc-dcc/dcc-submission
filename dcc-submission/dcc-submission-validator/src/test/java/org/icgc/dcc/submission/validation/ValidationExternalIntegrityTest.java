/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.icgc.dcc.submission.validation.platform.PlatformStrategy.SEPARATOR;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import lombok.val;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.JsonProcessingException;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.Relation;
import org.icgc.dcc.submission.dictionary.model.Term;
import org.icgc.dcc.submission.dictionary.model.ValueType;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.validation.core.Plan;
import org.icgc.dcc.submission.validation.platform.PlatformStrategy;
import org.icgc.dcc.submission.validation.platform.LocalPlatformStrategy;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;

public class ValidationExternalIntegrityTest extends BaseValidationIntegrityTest {

  /**
   * Test data.
   */
  private static final String ROOTDIR = "/fixtures/validation/external";
  private static final QueuedProject QUEUED_PROJECT = new QueuedProject("dummyProject", null);

  @Before
  public void setUp() throws JsonProcessingException, IOException {
    val termList1 = Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null));
    val termList2 = Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null));
    val termList3 =
        Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null), new Term("3", "dummy", null),
            new Term("4", "dummy", null), new Term("5", "dummy", null));
    val termList4 =
        Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null), new Term("3", "dummy", null));
    val termList5 =
        Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null), new Term("3", "dummy", null));

    when(context.getCodeList(anyString())).thenReturn(Optional.<CodeList> absent());

    when(context.getCodeList("GLOBAL.0.yes_no.v1")).thenReturn(Optional.of(codeList0));

    when(context.getCodeList("donor.0.donor_sex.v1")).thenReturn(Optional.of(codeList1));
    when(context.getCodeList("donor.0.donor_vital_status.v1")).thenReturn(Optional.of(codeList2));
    when(context.getCodeList("donor.0.disease_status_last_followup.v1")).thenReturn(Optional.of(codeList3));
    when(context.getCodeList("donor.0.donor_relapse_type.v1")).thenReturn(Optional.of(codeList4));

    when(context.getCodeList("specimen.0.specimen_type.v1")).thenReturn(Optional.of(codeList1));
    when(context.getCodeList("specimen.0.specimen_donor_treatment_type.v1")).thenReturn(
        Optional.of(codeList1));
    when(context.getCodeList("specimen.0.specimen_processing.v1")).thenReturn(Optional.of(codeList1));
    when(context.getCodeList("specimen.0.specimen_storage.v1")).thenReturn(Optional.of(codeList1));
    when(context.getCodeList("specimen.0.tumour_confirmed.v1")).thenReturn(Optional.of(codeList1));
    when(context.getCodeList("specimen.0.specimen_available.v1")).thenReturn(Optional.of(codeList1));

    when(context.getCodeList("sample.0.analyzed_sample_type.v1")).thenReturn(Optional.of(codeList5));

    when(codeList1.getTerms()).thenReturn(termList1);
    when(codeList2.getTerms()).thenReturn(termList2);
    when(codeList3.getTerms()).thenReturn(termList3);
    when(codeList4.getTerms()).thenReturn(termList4);
    when(codeList5.getTerms()).thenReturn(termList5);
  }

  @Test
  public void test_validate_valid() throws IOException, FilePresenceException {
    String content = validate(dictionary, ROOTDIR);
    assertTrue(content, content.isEmpty());

    String donorTrim = getUnsortedFileContent(ROOTDIR, "/.validation/donor" + SEPARATOR + "donor_id-offset.tsv");
    String donorTrimExpected = getUnsortedFileContent("/fixtures/validation/reference/fk_donor_trim.tsv");
    assertEquals("Incorrect donor ID trim list", donorTrimExpected.trim(), donorTrim.trim());

    String specimenTrim = getUnsortedFileContent(ROOTDIR, "/.validation/specimen" + SEPARATOR + "donor_id-offset.tsv");
    String specimenTrimExpected = getUnsortedFileContent("/fixtures/validation/reference/fk_specimen_trim.tsv");
    assertEquals("Incorrect specimen ID trim list", specimenTrimExpected.trim(), specimenTrim.trim());
  }

  @Test
  public void test_validate_invalidCompositeKeys() throws IOException, FilePresenceException {
    FileSchema donor = getFileSchemaByName(dictionary, "donor");
    FileSchema specimen = getFileSchemaByName(dictionary, "specimen");

    Field newFieldLeft = new Field();
    Field newFieldRight = new Field();
    newFieldLeft.setName("fakecolumn");
    newFieldLeft.setValueType(ValueType.TEXT);

    newFieldRight.setName("fakecolumn");
    newFieldRight.setValueType(ValueType.TEXT);
    donor.addField(newFieldLeft);
    specimen.addField(newFieldRight);

    String[] fieldNames = { "donor_id", "fakecolumn" };

    specimen.clearRelations();
    Relation relation = new Relation(Arrays.asList(fieldNames), "donor", Arrays.asList(fieldNames), false);
    specimen.addRelation(relation);

    testErrorType("fk_1");

    String donorTrim =
        getUnsortedFileContent(ROOTDIR, "/error/fk_1/.validation/donor" + SEPARATOR + "donor_id-fakecolumn-offset.tsv");
    String donorTrimExpected = getUnsortedFileContent("/fixtures/validation/reference/fk_1_donor_trim.tsv");
    assertEquals("Incorrect donor ID trim list", donorTrimExpected.trim(), donorTrim.trim());

    String specimenTrim =
        getUnsortedFileContent(ROOTDIR, "/error/fk_1/.validation/specimen" + SEPARATOR
            + "donor_id-fakecolumn-offset.tsv");
    String specimenTrimExpected = getUnsortedFileContent("/fixtures/validation/reference/fk_1_specimen_trim.tsv");
    assertEquals("Incorrect specimen ID trim list", specimenTrimExpected.trim(), specimenTrim.trim());
  }

  @Test(expected = FilePresenceException.class)
  public void test_validate_missingFile() throws IOException, FilePresenceException {
    testErrorType("fk_2");
  }

  private void testErrorType(String errorType) throws IOException, FilePresenceException {
    String content = validate(dictionary, ROOTDIR + "/error/" + errorType);
    String expected =
        FileUtils
            .readFileToString(new File(this.getClass()
                .getResource("/fixtures/validation/reference/" + errorType + ".json").getFile()));
    assertEquals(content, expected.trim(), content.trim());
  }

  private String validate(Dictionary dictionary, String relative) throws IOException, FilePresenceException {
    String rootDirString = this.getClass().getResource(relative).getFile();
    String outputDirString = rootDirString + "/" + ".validation";
    System.err.println(outputDirString);
    String errorFileString = outputDirString + "/" + "specimen.external" + SEPARATOR + "errors.json";

    File errorFile = new File(errorFileString);
    errorFile.delete();
    assertFalse(errorFileString, errorFile.exists());

    Path rootDir = new Path(rootDirString);
    Path outputDir = new Path(outputDirString);
    Path systemDir = SYSTEM_DIR;

    PlatformStrategy cascadingStrategy = new LocalPlatformStrategy(rootDir, outputDir, systemDir);

    Plan plan =
        validationService.planValidation(QUEUED_PROJECT, submissionDirectory, cascadingStrategy, dictionary,
            null);
    assertEquals(5, plan.getCascade().getFlows().size());

    plan.getCascade().complete();

    assertTrue(errorFileString, errorFile.exists());
    return FileUtils.readFileToString(errorFile);
  }

}
