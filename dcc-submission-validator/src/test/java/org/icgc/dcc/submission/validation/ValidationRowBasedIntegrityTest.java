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
package org.icgc.dcc.submission.validation;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.apache.commons.io.FileUtils.readFileToString;
import static org.icgc.dcc.submission.validation.platform.SubmissionPlatformStrategy.REPORT_FILES_INFO_SEPARATOR;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import lombok.SneakyThrows;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.model.DataType.DataTypes;
import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.Restriction;
import org.icgc.dcc.submission.dictionary.model.RestrictionType;
import org.icgc.dcc.submission.validation.cascading.ForbiddenValuesFunction;
import org.icgc.dcc.submission.validation.platform.LocalSubmissionPlatformStrategy;
import org.icgc.dcc.submission.validation.primary.core.Plan;
import org.icgc.dcc.submission.validation.primary.restriction.CodeListRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.DiscreteValuesRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.RangeFieldRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.RegexRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.RequiredRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.ScriptRestriction;
import org.icgc.dcc.submission.validation.primary.visitor.ValueTypePlanningVisitor;
import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;

public class ValidationRowBasedIntegrityTest extends BaseValidationIntegrityTest {

  /**
   * Test data.
   */
  private static final String ROOT_DIR = "/fixtures/validation/internal"; // TODO: rename to "row-based"
  private static final String PROJECT_KEY = "dummyProject";

  @Before
  public void setUp() throws Exception {
    val termList1 = terms(term("1"), term("2"));
    val termList2 = terms(term("1"), term("2"));
    val termList3 = terms(term("1"), term("2"), term("3"), term("4"), term("5"));
    val termList4 = terms(term("1"), term("2"), term("3"));
    val termList5 = terms(term("1"), term("2"), term("3"));

    when(context.getCodeList(anyString())).thenReturn(Optional.<CodeList> absent());

    when(context.getCodeList("donor.0.donor_sex.v1")).thenReturn(Optional.of(codeList1));
    when(context.getCodeList("donor.0.donor_vital_status.v1")).thenReturn(Optional.of(codeList2));
    when(context.getCodeList("donor.0.disease_status_last_followup.v1")).thenReturn(Optional.of(codeList3));
    when(context.getCodeList("donor.0.donor_relapse_type.v1")).thenReturn(Optional.of(codeList4));
    when(context.getCodeList("donor.0.prior_malignancy.v1")).thenReturn(Optional.of(codeList5));
    when(context.getCodeList("family.0.cancer_history_first_degree_relative.v1")).thenReturn(Optional.of(codeList5));

    when(codeList1.getTerms()).thenReturn(termList1);
    when(codeList2.getTerms()).thenReturn(termList2);
    when(codeList3.getTerms()).thenReturn(termList3);
    when(codeList4.getTerms()).thenReturn(termList4);
    when(codeList5.getTerms()).thenReturn(termList5);
  }

  @Test
  public void test_validate_valid() {
    String content = validate(dictionary, ROOT_DIR);
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
  public void test_validate_invalidScriptValues() {
    // Create restrictions
    val restriction1 = createScriptRestriction("donor_sex == 1", "Donor's sex must be male");
    val restriction2 = createScriptRestriction("donor_sex != 2", "Donor's sex must not equal two");
    val restrictions = Lists.<Restriction> newArrayList(restriction1, restriction2);

    // Create container
    val donor = getFileSchemaByName(dictionary, "donor");
    val donorSex = getFieldByName(donor, "donor_sex");
    donorSex.setRestrictions(restrictions);

    // Execute and verify
    testErrorType(ScriptRestriction.NAME);
  }

  private void testErrorType(String errorType) {
    val submissionFilePath = "/fixtures/validation/internal/error/" + errorType;
    val content = validate(dictionary, submissionFilePath);

    val expected = getResource("/fixtures/validation/reference/" + errorType + ".json");
    assertEquals("errorType = " + errorType + ", content = " + content, expected.trim(), content.trim());
  }

  @SneakyThrows
  private String validate(Dictionary dictionary, String submissionFilePath) {
    String rootDirString = this.getClass().getResource(submissionFilePath).getFile();
    String outputDirString = rootDirString + "/" + ".validation";
    String errorFileString = outputDirString + "/" + "donor.txt.internal" + REPORT_FILES_INFO_SEPARATOR + "errors.json";

    File errorFile = new File(errorFileString);
    errorFile.delete();
    assertFalse(errorFileString, errorFile.exists());

    Path rootDir = new Path(rootDirString);
    Path outputDir = new Path(outputDirString);

    val dataTypes = DataTypes.values();
    val platformStrategy = new LocalSubmissionPlatformStrategy(
        Collections.<String, String> emptyMap(), rootDir, outputDir);

    Plan plan = planner.plan(PROJECT_KEY, dataTypes, platformStrategy, dictionary);
    plan.connect();
    plan.getCascade().complete();

    assertTrue(errorFileString, errorFile.exists());
    return readFileToString(errorFile);
  }

  private static Restriction createScriptRestriction(String script, String description) {
    val config = new BasicDBObject();
    config.put(ScriptRestriction.PARAM, script);
    config.put(ScriptRestriction.PARAM_DESCRIPTION, description);

    val restriction = new Restriction();
    restriction.setType(RestrictionType.SCRIPT);
    restriction.setConfig(config);

    return restriction;
  }

}
