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

import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;

import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.FileSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Visits every {@code Dictionary}-related objects and creates an index of the elements for efficient lookup.
 * 
 */
public class DictionaryIndexVisitor extends BaseDictionaryVisitor {

  private static final Logger log = LoggerFactory.getLogger(DictionaryIndexVisitor.class);

  private final Map<String, FileSchema> fileSchemas;

  private final Map<String, Map<String, Field>> schemaToField;

  public DictionaryIndexVisitor() {
    this.fileSchemas = new TreeMap<String, FileSchema>();
    this.schemaToField = new TreeMap<String, Map<String, Field>>();
  }

  @Override
  public void visit(FileSchema fileSchema) {
    String schemaName = fileSchema.getName();
    log.info("index visiting fileSchema " + schemaName);
    if(this.fileSchemas.containsKey(schemaName)) {
      throw new DictionaryIndexException("Non-unique FileSchema name: " + schemaName);
    }
    this.fileSchemas.put(schemaName, fileSchema);
    for(Field field : fileSchema.getFields()) {
      String fieldName = field.getName();
      Map<String, Field> fields = this.schemaToField.get(schemaName);
      if(fields == null) {
        fields = new TreeMap<String, Field>();
      }
      if(fields.containsKey(fieldName)) {
        throw new DictionaryIndexException("Non-unique Field name " + fieldName + " in FileSchema " + schemaName);
      }
      fields.put(fieldName, field);
      this.schemaToField.put(schemaName, fields);
    }
  }

  public boolean hasFileSchema(String name) {
    return this.fileSchemas.containsKey(name);
  }

  public FileSchema getFileSchema(String name) {
    return this.fileSchemas.get(name);
  }

  public boolean hasField(String fileSchemaName, String fieldName) {
    Map<String, Field> fields = this.schemaToField.get(fileSchemaName);
    if(fields == null) {
      return false;
    }
    return fields.containsKey(fieldName);
  }

  public Field getField(String fileSchemaName, String fieldName) {
    Map<String, Field> fields = this.schemaToField.get(fileSchemaName);
    if(fields == null) {
      throw new DictionaryIndexException("FileSchema " + fileSchemaName + " does not exist in index");
    }
    return fields.get(fieldName);
  }

  public Iterable<String> getFieldNames(String fileSchemaName) {
    Map<String, Field> fields = this.schemaToField.get(fileSchemaName);
    if(fields == null) {
      throw new DictionaryIndexException("FileSchema " + fileSchemaName + " does not exist in index");
    }
    return new HashSet<String>(fields.keySet());
  }
}
