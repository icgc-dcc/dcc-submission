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
package org.icgc.dcc.submission.validation.key.report;

import static com.fasterxml.jackson.core.JsonGenerator.Feature.AUTO_CLOSE_TARGET;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static org.icgc.dcc.submission.core.report.Error.error;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.CONDITIONAL_RELATION;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.OPTIONAL_RELATION;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.RELATION;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.SURJECTION;
import static org.icgc.dcc.submission.validation.key.core.KVErrorType.UNIQUENESS;
import static org.icgc.dcc.submission.validation.key.surjectivity.SurjectivityValidator.SURJECTION_ERROR_LINE_NUMBER;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.core.report.Error;
import org.icgc.dcc.submission.validation.key.core.KVDictionary;
import org.icgc.dcc.submission.validation.key.core.KVErrorType;
import org.icgc.dcc.submission.validation.key.core.KVFileType;
import org.icgc.dcc.submission.validation.key.data.KVKey;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Optional;

/**
 * Reports key validation errors in the context of the submission system.
 */
@Slf4j
public class KVReporter implements Closeable {

  /**
   * The file name of the produced key validation report.
   */
  public static final String REPORT_FILE_NAME = "all.keys--errors.json";

  private final static ObjectWriter WRITER = new ObjectMapper(new JsonFactory().disable(AUTO_CLOSE_TARGET))
      .disable(FAIL_ON_EMPTY_BEANS).writer();

  @NonNull
  private final KVDictionary dictionary;
  @NonNull
  private final FileSystem fileSystem;
  @NonNull
  private final Path path;
  @NonNull
  private final OutputStream outputStream;

  @SneakyThrows
  public KVReporter(KVDictionary dictionary, FileSystem fileSystem, Path path) {
    this.dictionary = dictionary;
    this.fileSystem = fileSystem;
    this.path = path;
    this.outputStream = fileSystem.create(path);
  }

  @Override
  public void close() throws IOException {
    outputStream.close();
  }

  public void reportUniquenessError(KVFileType fileType, String fileName, long lineNumber, KVKey pk) {
    reportError(fileType, fileName, lineNumber, UNIQUENESS, pk, Optional.absent());
  }

  public void reportRelationError(KVFileType fileType, String fileName, long lineNumber, KVKey fk,
      Optional<KVFileType> referencedFileType) {
    reportError(fileType, fileName, lineNumber, RELATION, fk, referencedFileType);
  }

  public void reportOptionalRelationError(KVFileType fileType, String fileName, long lineNumber, KVKey optionalFk,
      Optional<KVFileType> referencedFileType) {
    reportError(fileType, fileName, lineNumber, OPTIONAL_RELATION, optionalFk, referencedFileType);
  }

  public void reportConditionalRelationError(KVFileType fileType, String fileName, long lineNumber,
      KVKey conditionalFk, Optional<KVFileType> referencedFileType) {
    reportError(fileType, fileName, lineNumber, CONDITIONAL_RELATION, conditionalFk, referencedFileType);
  }

  public void reportSurjectionError(KVFileType fileType, String fileName, KVKey keys, KVFileType referencedFileType) {
    reportError(fileType, fileName, SURJECTION_ERROR_LINE_NUMBER, SURJECTION, keys, Optional.of(referencedFileType));
  }

  private void reportError(KVFileType fileType, String fileName, long lineNumber, KVErrorType errorType, KVKey keys,
      Optional<KVFileType> referencedFileType) {
    log.debug("Reporting '{}' error at '({}, {}, {})': '{}'",
        new Object[] { errorType, fileType, fileName, lineNumber, keys });

    persistError(error()
        .fileName(fileName)
        .fieldNames(dictionary.getErrorFieldNames(fileType, errorType, referencedFileType))
        .params(getErrorParams(fileType, errorType, referencedFileType))
        .type(errorType.getErrorType())
        .lineNumber(lineNumber)
        .value(keys.getStringValues())
        .build());
  }

  @SneakyThrows
  private void persistError(Error error) {
    WRITER.writeValue(outputStream, error);
  }

  private Object[] getErrorParams(KVFileType fileType, KVErrorType errorType,
      Optional<KVFileType> optionalReferencedFileType) {
    // UNIQUENESS: uniqueness errors don't need params
    if (errorType == UNIQUENESS) {
      return null;
    }

    checkArgument(optionalReferencedFileType.isPresent(), "Expected referenced file type for RELATION, "
        + "OPTIONAL_RELATION and SURJECTION error types");
    val referencedFileType = optionalReferencedFileType.get();

    // RELATIONS:
    if (errorType == RELATION || errorType == OPTIONAL_RELATION || errorType == CONDITIONAL_RELATION) {
      val referencedFields = dictionary.getPrimaryKeyNames(referencedFileType);

      return new Object[] { referencedFileType, referencedFields };
    }

    // SURJECTION
    else if (errorType == SURJECTION) {
      val referencingFileType = fileType;
      val referencingFields = dictionary.getPrimaryKeyNames(referencedFileType);

      return new Object[] { referencingFileType, referencingFields };
    }

    throw new IllegalArgumentException(format("Unsupported error type %s", errorType));
  }

}
