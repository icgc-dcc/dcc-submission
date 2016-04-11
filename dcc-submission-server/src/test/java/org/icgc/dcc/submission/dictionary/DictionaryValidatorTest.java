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
package org.icgc.dcc.submission.dictionary;

import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.common.core.model.ValueType.INTEGER;
import static org.icgc.dcc.submission.dictionary.model.SummaryType.AVERAGE;

import java.util.List;

import org.icgc.dcc.submission.dictionary.model.CodeList;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.dictionary.model.Restriction;
import org.icgc.dcc.submission.dictionary.model.RestrictionType;
import org.icgc.dcc.submission.validation.primary.restriction.ScriptRestriction;
import org.junit.Test;

import com.mongodb.BasicDBObject;

import lombok.val;

public class DictionaryValidatorTest {

  @Test
  public void testValidateScriptRestriction() {
    // Build test dictionary bottom up
    val config = new BasicDBObject();
    config.put(ScriptRestriction.PARAM, "x == 1");
    config.put(ScriptRestriction.PARAM_DESCRIPTION, "x is one");

    val restriction = new Restriction();
    restriction.setType(RestrictionType.SCRIPT);
    restriction.setConfig(config);

    val field = new Field();
    field.setName("testField");
    field.setValueType(INTEGER);
    field.setLabel("Test field");
    field.setSummaryType(AVERAGE);
    field.addRestriction(restriction);

    val fileSchema = new FileSchema("testSchema");
    fileSchema.setPattern("testSchema");
    fileSchema.addField(field);

    val dictionary = new Dictionary();
    dictionary.addFile(fileSchema);

    // No code lists needed
    List<CodeList> codeLists = newArrayList();

    // Invoke
    val validator = new DictionaryValidator(dictionary, codeLists);
    val violations = validator.validate();

    // Verify
    assertThat(violations.getWarnings()).isEmpty();
    assertThat(violations.getErrors()).hasSize(3);
    for (val error : violations.getErrors()) {
      System.out.println(error);
    }
  }

}
