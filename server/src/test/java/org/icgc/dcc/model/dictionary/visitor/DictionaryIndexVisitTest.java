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
package org.icgc.dcc.model.dictionary.visitor;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.FileSchema;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

public class DictionaryIndexVisitTest {

  private static final String FILESCHEMA_NAME = "schema1";

  private static final String FIELD1_NAME = "field1";

  private static final String FIELD2_NAME = "field2";

  private FileSchema fileSchema;

  private Field field1;

  private Field field2;

  private DictionaryIndexVisitor visitor;

  @Before
  public void setup() {
    fileSchema = mock(FileSchema.class);
    field1 = mock(Field.class);
    field2 = mock(Field.class);

    List<Field> fieldList = new ArrayList<Field>();
    fieldList.add(field1);
    fieldList.add(field2);

    when(fileSchema.getName()).thenReturn(FILESCHEMA_NAME);
    when(fileSchema.getFields()).thenReturn(fieldList);

    when(field1.getName()).thenReturn(FIELD1_NAME);
    when(field2.getName()).thenReturn(FIELD2_NAME);

    visitor = new DictionaryIndexVisitor();
  }

  @Test
  public void test_visit_noErrors() {
    mockAccept(visitor);
  }

  @Test(expected = DictionaryIndexException.class)
  public void test_visit_nonUniqueSchemaName() {
    mockAccept(visitor);
    mockAccept(visitor);
  }

  @Test(expected = DictionaryIndexException.class)
  public void test_visit_nonUniqueFieldName() {
    Field duplicateField = mock(Field.class);
    when(duplicateField.getName()).thenReturn(FIELD1_NAME);
    fileSchema.getFields().add(duplicateField);

    mockAccept(visitor);
  }

  @Test
  public void test_hasFileSchema_returnsTrue() {
    mockAccept(visitor);
    assertTrue(visitor.hasFileSchema(FILESCHEMA_NAME));
  }

  @Test
  public void test_hasFileSchema_returnsFalse() {
    mockAccept(visitor);
    assertTrue(visitor.hasFileSchema("nonexistentSchema") == false);
  }

  @Test
  public void test_getFileSchema_returnsExpectedObject() {
    mockAccept(visitor);
    assertTrue(visitor.getFileSchema(FILESCHEMA_NAME).equals(this.fileSchema));
  }

  @Test
  public void test_getFileSchema_returnsNull() {
    mockAccept(visitor);
    assertTrue(visitor.getFileSchema("nonexistentSchema") == null);
  }

  @Test
  public void test_hasField_returnsTrue() {
    mockAccept(visitor);
    assertTrue(visitor.hasField(FILESCHEMA_NAME, FIELD1_NAME));
  }

  @Test
  public void test_hasField_returnsFalseForField() {
    mockAccept(visitor);
    assertTrue(visitor.hasField(FILESCHEMA_NAME, "nonexistentField") == false);
  }

  @Test
  public void test_hasField_returnsFalseForSchema() {
    mockAccept(visitor);
    assertTrue(visitor.hasField("nonexistentSchema", FIELD1_NAME) == false);
  }

  @Test
  public void test_getField_returnsExpectedField() {
    mockAccept(visitor);
    assertTrue(visitor.getField(FILESCHEMA_NAME, FIELD1_NAME).equals(field1));
  }

  @Test
  public void test_getField_returnsNull() {
    mockAccept(visitor);
    assertTrue(visitor.getField(FILESCHEMA_NAME, "nonexistentField") == null);
  }

  @Test(expected = DictionaryIndexException.class)
  public void test_getField_throwsException() {
    mockAccept(visitor);
    visitor.getField("nonexistentField", FIELD1_NAME);
  }

  @Test
  public void test_getFieldNames_containsExpected() {
    mockAccept(visitor);
    Set<String> fieldNames = Sets.newHashSet(visitor.getFieldNames(FILESCHEMA_NAME));
    for(Field field : fileSchema.getFields()) {
      assertTrue(fieldNames.contains(field.getName()));
    }
    assertTrue(fieldNames.contains("nonexistentField") == false);
  }

  @Test(expected = DictionaryIndexException.class)
  public void test_getFieldNames_throwsException() {
    mockAccept(visitor);
    visitor.getFieldNames("nonexistentField");
  }

  private void mockAccept(DictionaryIndexVisitor visitor) {
    visitor.visit(fileSchema);
    for(Field field : fileSchema.getFields()) {
      visitor.visit(field);
    }
  }
}
