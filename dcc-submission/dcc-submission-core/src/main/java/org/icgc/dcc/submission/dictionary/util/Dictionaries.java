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
package org.icgc.dcc.submission.dictionary.util;

import static com.google.common.io.Resources.getResource;
import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;

import java.util.regex.Pattern;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.google.common.base.Optional;

@NoArgsConstructor(access = PRIVATE)
public class Dictionaries {

  private static final ObjectReader FILE_SCHEMA_READER = new ObjectMapper().reader(FileSchema.class);
  private static final String FILE_SCHEMATA_PARENT_PATH = "dictionary";

  @SneakyThrows
  public static FileSchema readFileSchema(SubmissionFileType fileType) {
    val fileSchemaPath = format("%s/%s.json", FILE_SCHEMATA_PARENT_PATH, fileType.getTypeName());

    return FILE_SCHEMA_READER.readValue(getResource(fileSchemaPath));
  }

  public static Optional<FileSchema> getFileSchema(Dictionary dictionary, String fileName) {
    for (val schema : dictionary.getFiles()) {
      val match = Pattern.matches(schema.getPattern(), fileName);
      if (match) {
        return Optional.of(schema);
      }
    }

    return Optional.absent();
  }

  public static Optional<FeatureType> getFeatureType(FileSchema fileSchema) {
    val dataType = SubmissionFileType.from(fileSchema.getName()).getDataType();
    if (dataType.isFeatureType()) {
      val featureType = dataType.asFeatureType();

      return Optional.of(featureType);
    }

    return Optional.absent();
  }

}
