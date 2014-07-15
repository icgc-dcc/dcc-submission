/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.core.meta;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_UNDERSCORE;
import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.icgc.dcc.core.util.Resolver.DictionaryResolver;

import com.fasterxml.jackson.databind.JsonNode;

@RequiredArgsConstructor
public class MetadataGenerator {

  @NonNull
  private final DictionaryResolver resolver;

  public String generate() {
    val dictionary = resolver.getDictionary();

    StringBuilder b = new StringBuilder();
    b.append("package org.icgc.dcc.core.meta\n\n");
    b.append("import " + DataElement.class + ";\n\n");
    b.append("public class DictionaryMetadata {\n");

    for (val schema : dictionary.get("files")) {
      val schemaName = schema.get("name").asText();
      val className = getClassName(schemaName);

      b.append("\n  public static final " + className + " " + getInstanceName(className) + " = new " + className
          + "();\n\n");

      b.append(getClass(schema));
    }

    b.append("\n}\n");

    return b.toString();
  }

  private StringBuilder getClass(JsonNode schema) {
    StringBuilder b2 = new StringBuilder();
    val schemaName = schema.get("name").asText();
    val className = getClassName(schemaName);

    b2.append("  public static class " + className + " {\n\n");
    for (val field : schema.withArray("fields")) {
      val fieldName = field.get("name").asText();
      val fieldType = field.get("valueType").asText();
      b2.append("    public final DataElement " + getFieldName(fieldName) + " = new DataElement("
          + quote("dictionary")
          + "," + quote(schemaName) + "," + quote(fieldName) + "," + quote(fieldType) + "," + quote(fieldName)
          + ");\n");
    }

    b2.append("\n  }\n");

    return b2;
  }

  private String quote(String value) {
    return "\"" + value + "\"";
  }

  private String getClassName(String schemaName) {
    return LOWER_UNDERSCORE.to(UPPER_CAMEL, schemaName);
  }

  private String getInstanceName(String className) {
    return UPPER_CAMEL.to(LOWER_CAMEL, className);
  }

  private String getFieldName(String fieldName) {
    return LOWER_UNDERSCORE.to(LOWER_CAMEL, fieldName);
  }

}
