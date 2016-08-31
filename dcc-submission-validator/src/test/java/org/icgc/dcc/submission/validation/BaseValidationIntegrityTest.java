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

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Files.readLines;
import static java.util.Collections.sort;

import java.io.File;
import java.util.List;

import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.Term;
import org.icgc.dcc.submission.dictionary.util.Dictionaries;
import org.icgc.dcc.submission.fs.SubmissionFileSystem;
import org.icgc.dcc.submission.validation.primary.core.RestrictionContext;
import org.icgc.dcc.submission.validation.primary.core.RestrictionType;
import org.icgc.dcc.submission.validation.primary.planner.Planner;
import org.icgc.dcc.submission.validation.primary.restriction.CodeListRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.DiscreteValuesRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.RangeFieldRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.RegexRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.RequiredRestriction;
import org.icgc.dcc.submission.validation.primary.restriction.ScriptRestriction;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import lombok.SneakyThrows;
import lombok.val;

@RunWith(MockitoJUnitRunner.class)
public abstract class BaseValidationIntegrityTest {

  /**
   * Constants.
   */
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Mocks.
   */
  @Mock
  CodeList codeList0;
  @Mock
  CodeList codeList1;
  @Mock
  CodeList codeList2;
  @Mock
  CodeList codeList3;
  @Mock
  CodeList codeList4;
  @Mock
  CodeList codeList5;
  @Mock
  RestrictionContext context;
  @Mock
  SubmissionFileSystem submissionFileSystem;

  /**
   * Integration dependencies.
   */
  Dictionary dictionary;
  Planner planner;

  @Before
  public void before() {
    this.planner = new Planner(ImmutableSet.<RestrictionType> builder()
        .add(new DiscreteValuesRestriction.Type())
        .add(new RegexRestriction.Type())
        .add(new RangeFieldRestriction.Type())
        .add(new RequiredRestriction.Type())
        .add(new CodeListRestriction.Type(context))
        .add(new ScriptRestriction.Type())
        .build());

    this.dictionary = getDictionary();
  }

  protected static FileSchema getFileSchemaByName(Dictionary dictionary, String name) {
    for (val fileSchema : dictionary.getFiles()) {
      if (name.equals(fileSchema.getName())) {
        return fileSchema;
      }
    }

    return null;
  }

  protected static Field getFieldByName(FileSchema fileSchema, String name) {
    for (val field : fileSchema.getFields()) {
      if (name.equals(field.getName())) {
        return field;
      }
    }

    return null;
  }

  @SneakyThrows
  protected static Dictionary getDictionary() {
    return Dictionaries.readResourcesDictionary("0.11c");
  }

  @SneakyThrows
  protected static String getDictionaryText() {
    return MAPPER.writeValueAsString(getDictionary());
  }

  @SneakyThrows
  protected static String getResource(String resourcePath) {
    return Resources.toString(BaseValidationIntegrityTest.class.getResource(resourcePath), UTF_8);
  }

  @SneakyThrows
  protected static String getUnsortedFileContent(String resourcePath, String append) {
    val lines =
        readLines(new File(BaseValidationIntegrityTest.class.getResource(resourcePath).getFile() + append), UTF_8);
    sort(lines);

    return lines.toString();
  }

  protected static String getUnsortedFileContent(String resourcePath) {
    return getUnsortedFileContent(resourcePath, "");
  }

  protected Term term(String value) {
    return new Term(value, "dummy", null);
  }

  protected List<Term> terms(Term... terms) {
    return Lists.newArrayList(terms);
  }

}
