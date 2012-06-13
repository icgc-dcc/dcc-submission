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

import java.util.List;

import com.google.code.morphia.annotations.Embedded;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

/**
 * Describes a file schema that contains {@code Field}s and that is part of a {@code Dictionary}
 */
@Embedded
public class FileSchema {

  private final String name;

  private String label;

  private String pattern;

  private FileSchemaRole role;

  private List<String> uniqueFields;

  private List<Field> fields;

  public FileSchema(String name) {
    super();
    this.name = name;
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

  /**
   * @return the label
   */
  public String getLabel() {
    return label;
  }

  /**
   * @param label the label to set
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * @return the pattern
   */
  public String getPattern() {
    return pattern;
  }

  /**
   * @param pattern the pattern to set
   */
  public void setPattern(String pattern) {
    this.pattern = pattern;
  }

  /**
   * @return the role
   */
  public FileSchemaRole getRole() {
    return role;
  }

  /**
   * @param role the role to set
   */
  public void setRole(FileSchemaRole role) {
    this.role = role;
  }

  /**
   * @return the uniqueFields
   */
  public List<String> getUniqueFields() {
    return uniqueFields;
  }

  /**
   * @param uniqueFields the uniqueFields to set
   */
  public void setUniqueFields(List<String> uniqueFields) {
    this.uniqueFields = uniqueFields;
  }

  /**
   * @return the fields
   */
  public List<Field> getFields() {
    return fields;
  }

  /**
   * @param fields the fields to set
   */
  public void setFields(List<Field> fields) {
    this.fields = fields;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

}
