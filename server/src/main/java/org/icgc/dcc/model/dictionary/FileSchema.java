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
package org.icgc.dcc.model.dictionary;

import java.util.ArrayList;
import java.util.List;

import org.icgc.dcc.model.dictionary.visitor.DictionaryElement;
import org.icgc.dcc.model.dictionary.visitor.DictionaryVisitor;

import com.google.code.morphia.annotations.Embedded;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * Describes a file schema that contains {@code Field}s and that is part of a {@code Dictionary}
 */
@Embedded
public class FileSchema implements DictionaryElement {

  private String name;

  private String label;

  private String pattern;

  private FileSchemaRole role;

  private List<String> uniqueFields;

  private List<Field> fields;

  private Relation relation;

  public FileSchema() {
    super();
    this.uniqueFields = new ArrayList<String>();
    this.fields = new ArrayList<Field>();
  }

  public FileSchema(String name) {
    this();
    this.name = name;
  }

  @Override
  public void accept(DictionaryVisitor dictionaryVisitor) {
    dictionaryVisitor.visit(this);
    for(Field field : fields) {
      field.accept(dictionaryVisitor);
    }
  }

  public Optional<Field> field(final String name) {
    return Iterables.tryFind(fields, new Predicate<Field>() {

      @Override
      public boolean apply(Field input) {
        return input.getName().equals(name);
      }
    });
  }

  public Iterable<String> fieldNames() {
    return Iterables.transform(fields, new Function<Field, String>() {

      @Override
      public String apply(Field input) {
        return input.getName();
      }
    });
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getPattern() {
    return pattern;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  public FileSchemaRole getRole() {
    return role;
  }

  public void setRole(FileSchemaRole role) {
    this.role = role;
  }

  public List<String> getUniqueFields() {
    return uniqueFields;
  }

  public void setUniqueFields(List<String> uniqueFields) {
    this.uniqueFields = uniqueFields;
  }

  public List<Field> getFields() {
    return fields;
  }

  public void setFields(List<Field> fields) {
    this.fields = fields;
  }

  public void addField(Field field) {
    this.fields.add(field);
  }

  public String getName() {
    return name;
  }

  public boolean hasField(String fieldName) {
    for(Field field : this.fields) {
      if(field.getName().equals(fieldName)) {
        return true;
      }
    }
    return false;
  }

  public Relation getRelation() {
    return relation;
  }

  public void setRelation(Relation relation) {
    this.relation = relation;
  }
}
