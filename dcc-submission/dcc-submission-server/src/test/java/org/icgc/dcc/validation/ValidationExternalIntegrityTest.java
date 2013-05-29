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
package org.icgc.dcc.validation;

import static org.icgc.dcc.validation.CascadingStrategy.SEPARATOR;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.Path;
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
import org.icgc.dcc.filesystem.SubmissionDirectory;
import org.icgc.dcc.release.model.QueuedProject;
import org.icgc.dcc.validation.factory.LocalCascadingStrategyFactory;
import org.icgc.dcc.validation.service.ValidationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.io.Files;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.Inject;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ValidationTestModule.class })
public class ValidationExternalIntegrityTest { // TODO create base class for this and ValidationInternalIntegrityTest

  private static final String ROOTDIR = "/integration/validation/external";

  private static final QueuedProject QUEUED_PROJECT = new QueuedProject("dummyProject", null); // TODO: mock

  @Inject
  private DictionaryService dictionaryService;

  @Inject
  private Planner planner;

  private ValidationService validationService;

  private SubmissionDirectory submissionDirectory;

  private Dictionary dictionary;

  @Before
  public void setUp() throws JsonProcessingException, IOException {
    DccFileSystem dccFileSystem = mock(DccFileSystem.class);
    ProjectService projectService = mock(ProjectService.class);

    CodeList codeList1 = mock(CodeList.class);
    CodeList codeList2 = mock(CodeList.class);
    CodeList codeList3 = mock(CodeList.class);
    CodeList codeList4 = mock(CodeList.class);
    CodeList codeList5 = mock(CodeList.class);

    submissionDirectory = mock(SubmissionDirectory.class);

    List<Term> termList1 = Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null));
    List<Term> termList2 = Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null));
    List<Term> termList3 =
        Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null), new Term("3", "dummy", null),
            new Term("4", "dummy", null), new Term("5", "dummy", null));
    List<Term> termList4 =
        Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null), new Term("3", "dummy", null));
    List<Term> termList5 =
        Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null), new Term("3", "dummy", null));

    when(dictionaryService.getCodeList("dr__donor_sex")).thenReturn(Optional.of(codeList1));
    when(dictionaryService.getCodeList("dr__donor_vital_status")).thenReturn(Optional.of(codeList2));
    when(dictionaryService.getCodeList("dr__disease_status_last_followup")).thenReturn(Optional.of(codeList3));
    when(dictionaryService.getCodeList("dr__donor_relapse_type")).thenReturn(Optional.of(codeList4));

    when(dictionaryService.getCodeList("specimen__specimen_type")).thenReturn(Optional.of(codeList1));
    when(dictionaryService.getCodeList("specimen__specimen_donor_treatment_type")).thenReturn(Optional.of(codeList1));
    when(dictionaryService.getCodeList("specimen__specimen_processing")).thenReturn(Optional.of(codeList1));
    when(dictionaryService.getCodeList("specimen__specimen_storage")).thenReturn(Optional.of(codeList1));
    when(dictionaryService.getCodeList("specimen__tumour_confirmed")).thenReturn(Optional.of(codeList1));
    when(dictionaryService.getCodeList("specimen__specimen_available")).thenReturn(Optional.of(codeList1));

    when(dictionaryService.getCodeList("sp__analyzed_sample_type")).thenReturn(Optional.of(codeList5));

    when(codeList1.getTerms()).thenReturn(termList1);
    when(codeList2.getTerms()).thenReturn(termList2);
    when(codeList3.getTerms()).thenReturn(termList3);
    when(codeList4.getTerms()).thenReturn(termList4);
    when(codeList5.getTerms()).thenReturn(termList5);

    validationService =
        new ValidationService(dccFileSystem, projectService, planner, dictionaryService,
            new LocalCascadingStrategyFactory());

    resetDictionary();
  }

  @Test
  public void test_validate_valid() throws IOException, FilePresenceException {
    String content = validate(dictionary, ROOTDIR);
    Assert.assertTrue(content, content.isEmpty());

    String donorTrim = getUnsortedFileContent(ROOTDIR, "/.validation/donor" + SEPARATOR + "donor_id-offset.tsv");
    String donorTrimExpected = getUnsortedFileContent("/ref/fk_donor_trim.tsv");
    Assert.assertEquals("Incorrect donor ID trim list", donorTrimExpected.trim(), donorTrim.trim());

    String specimenTrim = getUnsortedFileContent(ROOTDIR, "/.validation/specimen" + SEPARATOR + "donor_id-offset.tsv");
    String specimenTrimExpected = getUnsortedFileContent("/ref/fk_specimen_trim.tsv");
    Assert.assertEquals("Incorrect specimen ID trim list", specimenTrimExpected.trim(), specimenTrim.trim());

    resetDictionary();
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

    resetDictionary();

    String donorTrim =
        getUnsortedFileContent(ROOTDIR, "/error/fk_1/.validation/donor" + SEPARATOR + "donor_id-fakecolumn-offset.tsv");
    String donorTrimExpected = getUnsortedFileContent("/ref/fk_1_donor_trim.tsv");
    Assert.assertEquals("Incorrect donor ID trim list", donorTrimExpected.trim(), donorTrim.trim());

    String specimenTrim =
        getUnsortedFileContent(ROOTDIR, "/error/fk_1/.validation/specimen" + SEPARATOR
            + "donor_id-fakecolumn-offset.tsv");
    String specimenTrimExpected = getUnsortedFileContent("/ref/fk_1_specimen_trim.tsv");
    Assert.assertEquals("Incorrect specimen ID trim list", specimenTrimExpected.trim(), specimenTrim.trim());
  }

  private String getUnsortedFileContent(String resourcePath) throws IOException {
    return getUnsortedFileContent(resourcePath, "");
  }

  private String getUnsortedFileContent(String resourcePath, String append) throws IOException {
    List<String> lines =
        Files.readLines(new File(this.getClass().getResource(resourcePath).getFile() + append), Charsets.UTF_8);
    Collections.sort(lines);
    return lines.toString();
  }

  @Test(expected = FilePresenceException.class)
  public void test_validate_missingFile() throws IOException, FilePresenceException {
    testErrorType("fk_2");
  }

  private void testErrorType(String errorType) throws IOException, FilePresenceException {
    String content = validate(dictionary, ROOTDIR + "/error/" + errorType);
    String expected =
        FileUtils.readFileToString(new File(this.getClass().getResource("/ref/" + errorType + ".json").getFile()));
    Assert.assertEquals(content, expected.trim(), content.trim());
  }

  private String validate(Dictionary dictionary, String relative) throws IOException, FilePresenceException {
    String rootDirString = this.getClass().getResource(relative).getFile();
    String outputDirString = rootDirString + "/" + ".validation";
    System.err.println(outputDirString);
    String errorFileString = outputDirString + "/" + "specimen.external" + SEPARATOR + "errors.json";

    File errorFile = new File(errorFileString);
    errorFile.delete();
    Assert.assertFalse(errorFileString, errorFile.exists());

    Path rootDir = new Path(rootDirString);
    Path outputDir = new Path(outputDirString);
    Path systemDir = new Path("src/test/resources/integrationtest/fs/SystemFiles");

    CascadingStrategy cascadingStrategy = new LocalCascadingStrategy(rootDir, outputDir, systemDir);

    TestCascadeListener listener = new TestCascadeListener();
    Plan plan =
        validationService.planAndConnectCascade(QUEUED_PROJECT, submissionDirectory, cascadingStrategy, dictionary,
            listener);
    Assert.assertEquals(5, plan.getCascade().getFlows().size());

    plan.startCascade();
    while(listener.isRunning()) {
      Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
    }
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
