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

import static org.icgc.dcc.submission.validation.CascadingStrategy.SEPARATOR;
import static org.icgc.dcc.submission.validation.restriction.RegexRestriction.NAME;
import static org.icgc.dcc.submission.validation.restriction.RegexRestriction.PARAM;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.filesystem.GuiceJUnitRunner;
import org.icgc.dcc.filesystem.GuiceJUnitRunner.GuiceModules;
import org.icgc.dcc.submission.core.ProjectService;
import org.icgc.dcc.submission.dictionary.DictionaryService;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.Restriction;
import org.icgc.dcc.submission.dictionary.model.Term;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.validation.CascadingStrategy;
import org.icgc.dcc.submission.validation.FilePresenceException;
import org.icgc.dcc.submission.validation.LocalCascadingStrategy;
import org.icgc.dcc.submission.validation.Plan;
import org.icgc.dcc.submission.validation.Planner;
import org.icgc.dcc.submission.validation.cascading.ForbiddenValuesFunction;
import org.icgc.dcc.submission.validation.factory.LocalCascadingStrategyFactory;
import org.icgc.dcc.submission.validation.restriction.CodeListRestriction;
import org.icgc.dcc.submission.validation.restriction.DiscreteValuesRestriction;
import org.icgc.dcc.submission.validation.restriction.RangeFieldRestriction;
import org.icgc.dcc.submission.validation.restriction.RequiredRestriction;
import org.icgc.dcc.submission.validation.service.ValidationService;
import org.icgc.dcc.submission.validation.visitor.UniqueFieldsPlanningVisitor;
import org.icgc.dcc.submission.validation.visitor.ValueTypePlanningVisitor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ValidationTestModule.class })
public class ValidationInternalIntegrityTest {

  private static final String ROOT_DIR = "/integration/validation/internal";

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

    submissionDirectory = mock(SubmissionDirectory.class);

    List<Term> termList1 = Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null));
    List<Term> termList2 = Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null));
    List<Term> termList3 =
        Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null), new Term("3", "dummy", null),
            new Term("4", "dummy", null), new Term("5", "dummy", null));
    List<Term> termList4 =
        Arrays.asList(new Term("1", "dummy", null), new Term("2", "dummy", null), new Term("3", "dummy", null));

    when(dictionaryService.getCodeList("dr__donor_sex")).thenReturn(Optional.of(codeList1));
    when(dictionaryService.getCodeList("dr__donor_vital_status")).thenReturn(Optional.of(codeList2));
    when(dictionaryService.getCodeList("dr__disease_status_last_followup")).thenReturn(Optional.of(codeList3));
    when(dictionaryService.getCodeList("dr__donor_relapse_type")).thenReturn(Optional.of(codeList4));

    when(codeList1.getTerms()).thenReturn(termList1);
    when(codeList2.getTerms()).thenReturn(termList2);
    when(codeList3.getTerms()).thenReturn(termList3);
    when(codeList4.getTerms()).thenReturn(termList4);

    validationService =
        new ValidationService(dccFileSystem, projectService, planner, dictionaryService,
            new LocalCascadingStrategyFactory());
    resetDictionary();
  }

  @Test
  public void test_validate_valid() throws IOException {
    String content = validate(validationService, dictionary, ROOT_DIR);
    Assert.assertTrue(content, content.isEmpty());
  }

  @Test
  public void test_validate_forbiddenValues() throws IOException {
    testErrorType(ForbiddenValuesFunction.NAME);
  }

  @Test
  public void test_validate_invalidValueType() throws IOException {
    testErrorType(ValueTypePlanningVisitor.NAME);
  }

  @Test
  public void test_validate_invalidCodeList() throws IOException {
    testErrorType(CodeListRestriction.NAME);
  }

  @Test
  public void test_validate_invalidRequired() throws IOException {
    testErrorType(RequiredRestriction.NAME);
  }

  @Test
  public void test_validate_invalidRange() throws IOException {
    BasicDBObject rangeConfig = new BasicDBObject();
    rangeConfig.put(RangeFieldRestriction.MIN, 0);
    rangeConfig.put(RangeFieldRestriction.MAX, 200);

    Restriction rangeRestriction = new Restriction(); // can't easily mock this because used by visitor as well
    rangeRestriction.setType(RangeFieldRestriction.NAME);
    rangeRestriction.setConfig(rangeConfig);

    // add a range restriction (none set at the moment); TODO: remove if range restrictions are added in the future
    FileSchema donor = getFileSchemaByName(dictionary, "donor");
    Field age = getFieldByName(donor, "donor_age_at_diagnosis");
    age.addRestriction(rangeRestriction);

    testErrorType(RangeFieldRestriction.NAME);

    resetDictionary();
  }

  @Test
  public void test_validate_invalidDiscreteValues() throws IOException {

    BasicDBObject inConfig = new BasicDBObject();
    inConfig.put(DiscreteValuesRestriction.PARAM, "CX,GL,FM");

    Restriction inRestriction = new Restriction();
    inRestriction.setType(DiscreteValuesRestriction.NAME);
    inRestriction.setConfig(inConfig);

    FileSchema donor = getFileSchemaByName(dictionary, "donor");
    Field region = getFieldByName(donor, "donor_region_of_residence");
    region.addRestriction(inRestriction);

    testErrorType(DiscreteValuesRestriction.NAME);

    resetDictionary();
  }

  @Test
  public void test_validate_invalidRegexValues() throws IOException {

    BasicDBObject config = new BasicDBObject();
    config.put(PARAM, "^T[0-9] N[0-9] M[0-9]$");

    Restriction restriction = new Restriction();
    restriction.setType(NAME);
    restriction.setConfig(config);

    FileSchema donor = getFileSchemaByName(dictionary, "donor");
    Field stage = getFieldByName(donor, "donor_tumour_stage_at_diagnosis");
    stage.setRestrictions(new ArrayList<Restriction>());
    stage.addRestriction(restriction);

    testErrorType(NAME);

    resetDictionary();
  }

  @Test
  public void test_validate_invalidUniqueFieldsCombination() throws IOException {
    FileSchema donor = getFileSchemaByName(dictionary, "donor");
    donor.setUniqueFields(Arrays.asList("donor_sex", "donor_region_of_residence", "donor_vital_status"));

    testErrorType(UniqueFieldsPlanningVisitor.NAME);

    resetDictionary();
  }

  private void testErrorType(String errorType) throws IOException {
    String content = validate(validationService, dictionary, "/integration/validation/internal/error/" + errorType);
    String expected =
        FileUtils.readFileToString(new File(this.getClass().getResource("/ref/" + errorType + ".json").getFile()));
    Assert.assertEquals(content, expected.trim(), content.trim());
  }

  private String validate(ValidationService validationService, Dictionary dictionary, String relative)
      throws IOException {
    String rootDirString = this.getClass().getResource(relative).getFile();
    String outputDirString = rootDirString + "/" + ".validation";
    String errorFileString = outputDirString + "/" + "donor.internal" + SEPARATOR + "errors.json";

    File errorFile = new File(errorFileString);
    errorFile.delete();
    Assert.assertFalse(errorFileString, errorFile.exists());

    Path rootDir = new Path(rootDirString);
    Path outputDir = new Path(outputDirString);
    Path systemDir = new Path("src/test/resources/integrationtest/fs/SystemFiles");

    CascadingStrategy cascadingStrategy = new LocalCascadingStrategy(rootDir, outputDir, systemDir);

    TestCascadeListener listener = new TestCascadeListener();
    Plan plan;
    try {
      plan =
          validationService.planAndConnectCascade(QUEUED_PROJECT, submissionDirectory, cascadingStrategy, dictionary,
              listener);
    } catch(FilePresenceException e) {
      throw new RuntimeException();
    }
    Assert.assertEquals(1, plan.getCascade().getFlows().size());

    plan.startCascade();
    while(listener.isRunning()) {
      Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
    }

    Assert.assertTrue(errorFileString, errorFile.exists());
    return FileUtils.readFileToString(errorFile);
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

  private Field getFieldByName(FileSchema fileSchema, String name) {
    Field field = null;
    for(Field fieldTmp : fileSchema.getFields()) {
      if(name.equals(fieldTmp.getName())) {
        field = fieldTmp;
        break;
      }
    }
    return field;
  }

  private void resetDictionary() throws IOException, JsonProcessingException {
    dictionary =
        new ObjectMapper().reader(Dictionary.class).readValue(
            new File(this.getClass().getResource("/dictionary.json").getFile()));
  }
}
