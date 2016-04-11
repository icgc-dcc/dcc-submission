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
package org.icgc.dcc.submission.dictionary.model;

import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedField;
import org.mongodb.morphia.mapping.MappingException;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public enum RestrictionType {

  CODELIST("codelist", false),
  DISCRETE_VALUES("in", false),
  RANGE("range", false),
  REGEX("regex", false),
  REQUIRED("required", false),
  SCRIPT("script", true);

  private final String id;

  /**
   * Allows multiple {@Restriction}s of the same type to be defined?
   */
  @Getter
  private final boolean multi;

  @JsonValue
  public String getId() {
    return id;
  }

  @JsonCreator
  public static RestrictionType byId(String id) {
    if (id == null) return null;
    for (val restriction : values()) {
      if (restriction.id.equals(id)) {
        return restriction;
      }
    }

    return null;
  }

  public static class RestrictionTypeConverter extends TypeConverter {

    public RestrictionTypeConverter() {
      super(RestrictionType.class);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object decode(Class targetClass, Object fromDBObject, MappedField optionalExtraInfo)
        throws MappingException {
      return RestrictionType.byId((String) fromDBObject);
    }

    @Override
    public Object encode(Object value, MappedField optionalExtraInfo) {
      if (value == null) return null;
      return ((RestrictionType) value).getId();
    }

  }

}
