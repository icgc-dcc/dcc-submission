/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.validation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.core.ProjectService;
import org.icgc.dcc.dictionary.DictionaryService;
import org.icgc.dcc.dictionary.model.CodeList;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.dictionary.model.Field;
import org.icgc.dcc.dictionary.model.FileSchema;
import org.icgc.dcc.dictionary.model.Relation;
import org.icgc.dcc.dictionary.model.Term;
import org.icgc.dcc.dictionary.model.ValueType;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.GuiceJUnitRunner;
import org.icgc.dcc.filesystem.GuiceJUnitRunner.GuiceModules;
import org.icgc.dcc.validation.service.ValidationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ValidationTestModule.class })
public class ValidationExternalIntegrityTest {

  private static final String ROOTDIR = "/integration/validation/external";

  @Inject
  private DictionaryService dictionaryService;

  @Inject
  private Planner planner;

  private ValidationService validationService;

  private Dictionary dictionary;

  @Before
  public void setUp() throws JsonProcessingException, IOException {
    DccFileSystem dccFileSystem = mock(DccFileSystem.class);
    ProjectService projectService = mock(ProjectService.class);

    CodeList codeList1 = mock(CodeList.class);
    CodeList codeList2 = mock(CodeList.class);
    CodeList codeList3 = mock(CodeList.class);
    CodeList codeList4 = mock(CodeList.class);

    List<Term> termList1 = Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null));
    List<Term> termList2 = Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null));
    List<Term> termList3 =
        Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null), new Term("3", "dummy", null),
            new Term("4", "dummy", null), new Term("5", "dummy", null));
    List<Term> termList4 =
        Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null), new Term("3", "dummy", null));

    when(dictionaryService.getCodeList("dr__donor_sex")).thenReturn(codeList1);
    when(dictionaryService.getCodeList("dr__donor_vital_status")).thenReturn(codeList2);
    when(dictionaryService.getCodeList("dr__disease_status_last_followup")).thenReturn(codeList3);
    when(dictionaryService.getCodeList("dr__donor_relapse_type")).thenReturn(codeList4);

    when(dictionaryService.getCodeList("specimen__specimen_type")).thenReturn(codeList1);
    when(dictionaryService.getCodeList("specimen__specimen_donor_treatment_type")).thenReturn(codeList1);
    when(dictionaryService.getCodeList("specimen__specimen_processing")).thenReturn(codeList1);
    when(dictionaryService.getCodeList("specimen__specimen_storage")).thenReturn(codeList1);
    when(dictionaryService.getCodeList("specimen__tumour_confirmed")).thenReturn(codeList1);
    when(dictionaryService.getCodeList("specimen__specimen_available")).thenReturn(codeList1);

    when(codeList1.getTerms()).thenReturn(termList1);
    when(codeList2.getTerms()).thenReturn(termList2);
    when(codeList3.getTerms()).thenReturn(termList3);
    when(codeList4.getTerms()).thenReturn(termList4);

    validationService = new ValidationService(dccFileSystem, projectService, planner, dictionaryService);

    resetDictionary();
  }

  @Test
  public void test_validate_valid() throws IOException {
    String content = validate(validationService, dictionary, ROOTDIR);
    Assert.assertTrue(content, content.isEmpty());

    String donorTrim =
        FileUtils.readFileToString(new File(this.getClass().getResource(ROOTDIR).getFile()
            + "/.validation/donor#donor_id.tsv"));
    String donorTrimExpected =
        FileUtils.readFileToString(new File(this.getClass().getResource("/ref/fk_donor_trim.tsv").getFile()));
    Assert.assertEquals("Incorrect donor ID trim list", donorTrimExpected.trim(), donorTrim.trim());

    String specimenTrim =
        FileUtils.readFileToString(new File(this.getClass().getResource(ROOTDIR).getFile()
            + "/.validation/specimen#donor_id.tsv"));
    String specimenTrimExpected =
        FileUtils.readFileToString(new File(this.getClass().getResource("/ref/fk_specimen_trim.tsv").getFile()));
    Assert.assertEquals("Incorrect specimen ID trim list", specimenTrimExpected.trim(), specimenTrim.trim());
  }

  @Test
  public void test_validate_invalidCompositeKeys() throws IOException {
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

    specimen.getRelation().clear();
    Relation relation = new Relation(Arrays.asList(fieldNames), "donor", Arrays.asList(fieldNames));
    specimen.addRelation(relation);

    testErrorType("fk_1");

    resetDictionary();

    String donorTrim =
        FileUtils.readFileToString(new File(this.getClass().getResource(ROOTDIR).getFile()
            + "/error/fk_1/.validation/donor#donor_id-fakecolumn.tsv"));
    String donorTrimExpected =
        FileUtils.readFileToString(new File(this.getClass().getResource("/ref/fk_1_donor_trim.tsv").getFile()));
    Assert.assertEquals("Incorrect donor ID trim list", donorTrimExpected.trim(), donorTrim.trim());

    String specimenTrim =
        FileUtils.readFileToString(new File(this.getClass().getResource(ROOTDIR).getFile()
            + "/error/fk_1/.validation/specimen#donor_id-fakecolumn.tsv"));
    String specimenTrimExpected =
        FileUtils.readFileToString(new File(this.getClass().getResource("/ref/fk_1_specimen_trim.tsv").getFile()));
    Assert.assertEquals("Incorrect specimen ID trim list", specimenTrimExpected.trim(), specimenTrim.trim());
  }

  @Test(expected = PlannerException.class)
  public void test_validate_missingFile() throws IOException {
    testErrorType("fk_2");
  }

  private void testErrorType(String errorType) throws IOException {
    String content = validate(validationService, dictionary, ROOTDIR + "/error/" + errorType);
    String expected =
        FileUtils.readFileToString(new File(this.getClass().getResource("/ref/" + errorType + ".json").getFile()));
    Assert.assertEquals(content, expected.trim(), content.trim());
  }

  private String validate(ValidationService validationService, Dictionary dictionary, String relative)
      throws IOException {
    String rootDirString = this.getClass().getResource(relative).getFile();
    String outputDirString = rootDirString + "/" + ".validation";
    System.err.println(outputDirString);
    String errorFileString = outputDirString + "/" + "specimen.external#errors.json";

    File errorFile = new File(errorFileString);
    errorFile.delete();
    Assert.assertFalse(errorFileString, errorFile.exists());

    File rootDir = new File(rootDirString);
    File outputDir = new File(outputDirString);

    CascadingStrategy cascadingStrategy = new LocalCascadingStrategy(rootDir, outputDir);

    Plan plan = validationService.planCascade(null, cascadingStrategy, dictionary);
    Assert.assertEquals(5, plan.getCascade().getFlows().size());
    validationService.runCascade(plan.getCascade(), null);

    Assert.assertTrue(errorFileString, errorFile.exists());
    return FileUtils.readFileToString(errorFile);
  }

  private void resetDictionary() throws IOException, JsonProcessingException {
    dictionary =
        new ObjectMapper().reader(Dictionary.class).readValue(
            new File(this.getClass().getResource("/dictionary.json").getFile()));
  }

  private FileSchema getFileSchemaByName(Dictionary dictionary, String name) {
    FileSchema fileSchema = null;
    for(FileSchema fileSchemaTmp : dictionary.getFiles()) {
      if(name.equals(fileSchemaTmp.getName())) {
        fileSchema = fileSchemaTmp;
        break;
      }
    }
    return fileSchema;
  }
}
