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
package org.icgc.dcc.generator.utils;

import static com.google.common.io.Resources.getResource;
import static lombok.AccessLevel.PRIVATE;

import java.net.URL;
import java.util.List;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.Field;
import org.icgc.dcc.submission.dictionary.model.Restriction;
import org.icgc.dcc.submission.dictionary.model.RestrictionType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

/**
 * Dictionary utilitis to verify common properties of fields.
 */
@NoArgsConstructor(access = PRIVATE)
public final class Dictionaries {

  /**
   * General constants.
   */
  private static final String SCHEMA_TYPE_SEPARATOR = "_";

  /**
   * Restriction constants.
   */
  private static final String ACCEPT_MISSING_CODE_TYPE = "acceptMissingCode";

  /**
   * Schema name constants.
   */
  public static final String DONOR_SCHEMA_NAME = "donor";
  public static final String SPECIMEN_SCHEMA_NAME = "specimen";
  public static final String SAMPLE_SCHEMA_NAME = "sample";

  /**
   * Dictionary constants.
   */
  private static final ObjectReader READER = new ObjectMapper().reader(Dictionary.class);
  private static final String DEFAULT_DICTIONARY_PATH = "org/icgc/dcc/resources/Dictionary.json";
  public static final URL DEFAULT_DICTIONARY_URL = getResource(DEFAULT_DICTIONARY_PATH);

  public static boolean isUniqueField(List<String> uniqueFields, String fieldName) {
    return uniqueFields.contains(fieldName);
  }

  public static boolean isRequired(Field field) {
    return field.getRestriction(RestrictionType.REQUIRED).isPresent();
  }

  public static boolean isCodeListField(Field field) {
    return field.getRestriction(RestrictionType.CODELIST).isPresent();
  }

  public static boolean isMissingCodeAccepted(Field field) {
    Restriction restriction = field.getRestriction(RestrictionType.REQUIRED).get();
    return restriction.getConfig().getBoolean(ACCEPT_MISSING_CODE_TYPE);
  }

  public static String getSchemaType(String schemaName) {
    return (schemaName.indexOf(SCHEMA_TYPE_SEPARATOR) != -1 ? schemaName.substring(schemaName.length() - 1) : schemaName);
  }

  public static String getSchemaName(String schemaName) {
    return (schemaName.indexOf(SCHEMA_TYPE_SEPARATOR) != -1 ? schemaName.substring(0, schemaName.length() - 2) : schemaName);
  }

  public static String getDictionaryVerision() {
    return getDictionary().getVersion();
  }

  public static Dictionary getDictionary() {
    return resolveDictionary(DEFAULT_DICTIONARY_URL);
  }

  public static Dictionary getDictionary(URL url) {
    return resolveDictionary(url);
  }

  @SneakyThrows
  private static Dictionary resolveDictionary(URL url) {
    return READER.readValue(url);
  }

}