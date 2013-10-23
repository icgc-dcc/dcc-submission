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
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.icgc.dcc.submission.TestUtils.dictionaryToString;
import static org.icgc.dcc.submission.TestUtils.resourceToString;
import static org.icgc.dcc.submission.validation.CascadingStrategy.SEPARATOR;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import lombok.SneakyThrows;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.submission.dictionary.DictionaryService;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.Restriction;
import org.icgc.dcc.submission.dictionary.model.RestrictionType;
import org.icgc.dcc.submission.dictionary.model.Term;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.GuiceJUnitRunner;
import org.icgc.dcc.submission.fs.GuiceJUnitRunner.GuiceModules;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.release.model.QueuedProject;
import org.icgc.dcc.submission.validation.cascading.ForbiddenValuesFunction;
import org.icgc.dcc.submission.validation.factory.LocalCascadingStrategyFactory;
import org.icgc.dcc.submission.validation.restriction.CodeListRestriction;
import org.icgc.dcc.submission.validation.restriction.DiscreteValuesRestriction;
import org.icgc.dcc.submission.validation.restriction.RangeFieldRestriction;
import org.icgc.dcc.submission.validation.restriction.RegexRestriction;
import org.icgc.dcc.submission.validation.restriction.RequiredRestriction;
import org.icgc.dcc.submission.validation.restriction.ScriptRestriction;
import org.icgc.dcc.submission.validation.service.ValidationService;
import org.icgc.dcc.submission.validation.visitor.UniqueFieldsPlanningVisitor;
import org.icgc.dcc.submission.validation.visitor.ValueTypePlanningVisitor;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;

@RunWith(GuiceJUnitRunner.class)
@GuiceModules({ ValidationTestModule.class })
public class ValidationInternalIntegrityTest {

  /**
   * Test data.
   */
  private static final String ROOT_DIR = "/fixtures/validation/internal";
  private static final QueuedProject QUEUED_PROJECT = new QueuedProject("dummyProject", null); // TODO: mock

  /**
   * Dependencies.
   */
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

    when(dictionaryService.getCodeList(anyString())).thenReturn(Optional.<CodeList> absent());

    when(dictionaryService.getCodeList("donor.0.donor_sex.v1")).thenReturn(Optional.of(codeList1));
    when(dictionaryService.getCodeList("donor.0.donor_vital_status.v1")).thenReturn(Optional.of(codeList2));
    when(dictionaryService.getCodeList("donor.0.disease_status_last_followup.v1")).thenReturn(Optional.of(codeList3));
    when(dictionaryService.getCodeList("donor.0.donor_relapse_type.v1")).thenReturn(Optional.of(codeList4));

    when(codeList1.getTerms()).thenReturn(termList1);
    when(codeList2.getTerms()).thenReturn(termList2);
    when(codeList3.getTerms()).thenReturn(termList3);
    when(codeList4.getTerms()).thenReturn(termList4);

    validationService =
        new ValidationService(planner, dccFileSystem, dictionaryService, new LocalCascadingStrategyFactory());

    dictionary = createDictionary();
  }

  @Test
  public void test_validate_valid() {
    String content = validate(validationService, dictionary, ROOT_DIR);
    assertTrue(content, content.isEmpty());
  }

  @Test
  public void test_validate_forbiddenValues() {
    testErrorType(ForbiddenValuesFunction.NAME);
  }

  @Test
  public void test_validate_invalidValueType() {
    testErrorType(ValueTypePlanningVisitor.NAME);
  }

  @Test
  public void test_validate_invalidCodeList() {
    testErrorType(CodeListRestriction.NAME);
  }

  @Test
  public void test_validate_invalidRequired() {
    testErrorType(RequiredRestriction.NAME);
  }

  @Test
  public void test_validate_invalidRange() {
    BasicDBObject rangeConfig = new BasicDBObject();
    rangeConfig.put(RangeFieldRestriction.MIN, 0);
    rangeConfig.put(RangeFieldRestriction.MAX, 200);

    Restriction rangeRestriction = new Restriction(); // can't easily mock this because used by visitor as well
    rangeRestriction.setType(RestrictionType.RANGE);
    rangeRestriction.setConfig(rangeConfig);

    // add a range restriction (none set at the moment); TODO: remove if range restrictions are added in the future
    FileSchema donor = getFileSchemaByName(dictionary, "donor");
    Field age = getFieldByName(donor, "donor_age_at_diagnosis");
    age.addRestriction(rangeRestriction);

    testErrorType(RangeFieldRestriction.NAME);
  }

  @Test
  public void test_validate_invalidDiscreteValues() {
    BasicDBObject inConfig = new BasicDBObject();
    inConfig.put(DiscreteValuesRestriction.PARAM, "CX,GL,FM");

    Restriction inRestriction = new Restriction();
    inRestriction.setType(RestrictionType.DISCRETE_VALUES);
    inRestriction.setConfig(inConfig);

    FileSchema donor = getFileSchemaByName(dictionary, "donor");
    Field region = getFieldByName(donor, "donor_region_of_residence");
    region.addRestriction(inRestriction);

    testErrorType(DiscreteValuesRestriction.NAME);
  }

  @Test
  public void test_validate_invalidRegexValues() {
    BasicDBObject config = new BasicDBObject();
    config.put(RegexRestriction.PARAM, "^T[0-9] N[0-9] M[0-9]$");

    Restriction restriction = new Restriction();
    restriction.setType(RestrictionType.REGEX);
    restriction.setConfig(config);

    FileSchema donor = getFileSchemaByName(dictionary, "donor");
    Field stage = getFieldByName(donor, "donor_tumour_stage_at_diagnosis");
    stage.setRestrictions(new ArrayList<Restriction>());
    stage.addRestriction(restriction);

    testErrorType(RegexRestriction.NAME);
  }

  @Test
  @Ignore
  public void test_validate_invalidScriptValues() {
    BasicDBObject config = new BasicDBObject();
    config.put(ScriptRestriction.NAME, "donor_sex == 1");

    Restriction restriction = new Restriction();
    restriction.setType(RestrictionType.SCRIPT);
    restriction.setConfig(config);

    FileSchema donor = getFileSchemaByName(dictionary, "donor");
    Field stage = getFieldByName(donor, "donor_sex");
    stage.setRestrictions(new ArrayList<Restriction>());
    stage.addRestriction(restriction);

    testErrorType(ScriptRestriction.NAME);
  }

  @Test
  public void test_validate_invalidUniqueFieldsCombination() {
    FileSchema donor = getFileSchemaByName(dictionary, "donor");
    donor.setUniqueFields(Arrays.asList("donor_sex", "donor_region_of_residence", "donor_vital_status"));

    testErrorType(UniqueFieldsPlanningVisitor.NAME);
  }

  private void testErrorType(String errorType) {
    String content = validate(validationService, dictionary, "/fixtures/validation/internal/error/" + errorType);

    String expected = resourceToString("/fixtures/validation/reference/" + errorType + ".json");
    assertEquals("errorType = " + errorType + ", content = " + content, expected.trim(), content.trim());
  }

  @SneakyThrows
  private String validate(ValidationService validationService, Dictionary dictionary, String relative) {
    String rootDirString = this.getClass().getResource(relative).getFile();
    String outputDirString = rootDirString + "/" + ".validation";
    String errorFileString = outputDirString + "/" + "donor.internal" + SEPARATOR + "errors.json";

    File errorFile = new File(errorFileString);
    errorFile.delete();
    assertFalse(errorFileString, errorFile.exists());

    Path rootDir = new Path(rootDirString);
    Path outputDir = new Path(outputDirString);
    Path systemDir = new Path("src/test/resources/fixtures/submission/fs/SystemFiles");

    CascadingStrategy cascadingStrategy = new LocalCascadingStrategy(rootDir, outputDir, systemDir);

    Plan plan = validationService.planValidation(
        QUEUED_PROJECT, submissionDirectory, cascadingStrategy, dictionary, null);
    assertEquals(1, plan.getCascade().getFlows().size());

    plan.getCascade().complete();

    assertTrue(errorFileString, errorFile.exists());
    return readFileToString(errorFile);
  }

  private static FileSchema getFileSchemaByName(Dictionary dictionary, String name) {
    for (val fileSchema : dictionary.getFiles()) {
      if (name.equals(fileSchema.getName())) {
        return fileSchema;
      }
    }

    return null;
  }

  private static Field getFieldByName(FileSchema fileSchema, String name) {
    for (val field : fileSchema.getFields()) {
      if (name.equals(field.getName())) {
        return field;
      }
    }

    return null;
  }

  @SneakyThrows
  private static Dictionary createDictionary() {
    return new ObjectMapper().reader(Dictionary.class).readValue(dictionaryToString());
  }

}
