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
package org.icgc.dcc.submission.validation.accession;

import static org.icgc.dcc.common.core.util.Splitters.COLON;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableList;
import static org.icgc.dcc.submission.core.parser.SubmissionFileParsers.newMapFileParser;
import static org.icgc.dcc.submission.core.report.Error.error;
import static org.icgc.dcc.submission.validation.accession.core.AccessionFields.RAW_DATA_ACCESSION_FIELD_NAME;
import static org.icgc.dcc.submission.validation.accession.core.AccessionFields.getAnalysisId;
import static org.icgc.dcc.submission.validation.accession.core.AccessionFields.getAnalyzedSampleId;
import static org.icgc.dcc.submission.validation.accession.core.AccessionFields.getRawDataAccession;
import static org.icgc.dcc.submission.validation.accession.core.AccessionFields.getRawDataRepository;
import static org.icgc.dcc.submission.validation.core.Validators.checkInterrupted;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.common.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.common.ega.model.EGAAccessionType;
import org.icgc.dcc.common.hadoop.parser.FileParser;
import org.icgc.dcc.submission.core.report.ErrorType;
import org.icgc.dcc.submission.dictionary.model.Term;
import org.icgc.dcc.submission.dictionary.util.CodeLists;
import org.icgc.dcc.submission.validation.accession.core.AccessionDictionary;
import org.icgc.dcc.submission.validation.accession.ega.EGAFileAccessionValidator;
import org.icgc.dcc.submission.validation.cascading.TupleState;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.rgv.report.TupleStateWriter;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Validates the presence and existence of accessions.
 * <p>
 * Accessions are defined in experimental meta files (e.g. {@code ssm_p}, {@code cnsm_p}, etc.). Each meta file has a
 * similar structure which involves 3 fields for accession validation:
 * <ul>
 * <li>{@code analysis_id}</li>
 * <li>{@code raw_data_repository}</li>
 * <li>{@code raw_data_accession}</li>
 * </ul>
 * Due to the effort involved in applying this new validator to existing data, it has been decided to "grandfather"
 * existing invalid data and exclude these accessions on the basis of the {@code analysis_id}.
 * <p>
 * Note that only EGA file accession is currently supported. The push for EGA validation was to ensure that files are
 * actually submitted. Historically this has not been the case. Furthermore, EGA submissions have only been submitting
 * study and dataset accessions. File accessions are a new feature (circa 2015) of EGA.
 * <p>
 * Assumes that files are welformed (FPV has run) and primary validation has been performed. Should be scheduled as late
 * as possible as to not block other validators from reporting upstream errors.
 * 
 * @see <span>Dictionary <a href="http://docs.icgc.org/dictionary/viewer/#?viewMode=table&q=raw_data%7Canalysis_id">meta
 * files</a></span>
 */
@Slf4j
@RequiredArgsConstructor
public class AccessionValidator implements Validator {

  /**
   * Dependencies.
   */
  @NonNull
  private final AccessionDictionary dictionary;
  @NonNull
  private final EGAFileAccessionValidator egaValidator;

  @Override
  public String getName() {
    return "Accession Validator";
  }

  @Override
  public void validate(@NonNull ValidationContext context) throws InterruptedException {
    log.info("Starting...");

    // Selective validation filtering
    if (!isValidatable(context)) {
      log.info("Validation not required for '{}'. Skipping...", context.getProjectKey());

      return;
    }

    validateMeta(context);
  }

  private void validateMeta(ValidationContext context) {
    // Find all validatable, requested meta file types
    val metaFileTypes = resolveMetaFileTypes(context);

    for (val metaFileType : metaFileTypes) {
      try {
        val metaFiles = context.getFiles(metaFileType);
        if (metaFiles.isEmpty()) {
          // This could happen if the request asks for types which don't exist. Luckily, this only practically happens
          // during testing (e.g. {@link SubmissionIntegrationTest}) since asking for all data types to validate is
          // easier to program. Ideally this check would be pushed upstream to {@link
          // ValidationService#createValidationContext} so that all validators wouldn't have to repeat it. However, we
          // must be careful not to exclude validation of existence of clinical by doing so.
          log.debug("No meta files of type {}, skipping...", metaFileType);
          continue;
        }

        val metaFileParser = createMetaFileParser(context, metaFileType);
        validateMetaFileType(context, metaFileType, metaFiles, metaFileParser);
      } catch (Exception e) {
        log.error("Error validating file type: {}: ", metaFileType, e);
        throw e;
      }
    }
  }

  private void validateMetaFileType(ValidationContext context, FileType metaFileType, List<Path> metaFiles,
      FileParser<Map<String, String>> metaFileParser) {
    for (val metaFile : metaFiles) {
      try {
        // TODO: Verify that this is required
        @Cleanup
        val writer = createTupleStateWriter(context, metaFile);

        // Get to work
        log.info("Performing accession validation on meta file '{}' for '{}'", metaFile, context.getProjectKey());
        validateMetaFile(context, metaFileType, metaFile, metaFileParser, writer);
        log.info("Finished performing accession validation for '{}'", context.getProjectKey());
      } catch (Exception e) {
        throw new RuntimeException("Error validating accession: meta file " + metaFile, e);
      }
    }
  }

  @SneakyThrows
  private void validateMetaFile(ValidationContext context, FileType fileType, Path filePath,
      FileParser<Map<String, String>> fileParser, TupleStateWriter writer) {
    // Validate all records
    fileParser.parse(filePath, (long lineNumber, Map<String, String> record) -> validateMetaFileRecord(
        context, writer, fileType, filePath.getName(), lineNumber, record, resolveEGATerm(context)));
  }

  private void validateMetaFileRecord(ValidationContext context, TupleStateWriter writer, FileType fileType,
      String fileName,
      long lineNumber, Map<String, String> record, Term egaTerm) throws IOException {
    // Cooperate
    checkInterrupted(getName());

    // Currently only EGA validation is supported
    val rawDataRepository = getRawDataRepository(record);
    if (!isEGA(egaTerm, rawDataRepository)) {
      return;
    }

    // Access field values required for validation
    val analysisId = getAnalysisId(record);
    val analyzedSampleId = getAnalyzedSampleId(record);
    val rawDataAccession = getRawDataAccession(record);

    // Apply whitelist to exclude historical "grandfathered" records
    if (dictionary.isExcluded(context.getProjectKey(), fileType, analysisId, analyzedSampleId)) {
      return;
    }

    // Parse and resolve only the the file accessions
    val fileIds = resolveFileAccessions(rawDataAccession);

    // [Presence] Ensure at least one file accession is specified
    if (fileIds.isEmpty()) {
      val type = ErrorType.FILE_ACCESSION_MISSING;
      val value = rawDataAccession;
      val columnName = RAW_DATA_ACCESSION_FIELD_NAME;
      val param = analysisId;

      reportError(context, writer, fileName, lineNumber, type, value, columnName, param);

      return;
    }

    // [Existence] Ensure file accession exists when specified
    for (val fileId : fileIds) {
      val result = egaValidator.validate(fileId);
      if (!result.isValid()) {
        val type = ErrorType.FILE_ACCESSION_INVALID;
        val value = rawDataRepository;
        val columnName = RAW_DATA_ACCESSION_FIELD_NAME;
        val param = result.getReason();

        reportError(context, writer, fileName, lineNumber, type, value, columnName, param);
      }
    }
  }

  private static boolean isValidatable(ValidationContext context) {
    return context.getDataTypes().stream().anyMatch(DataType::isFeatureType);
  }

  private static void reportError(ValidationContext context, TupleStateWriter writer, String fileName, long lineNumber,
      ErrorType type, String value, String columnName, String param) throws IOException {
    // Database
    context.reportError(
        error()
            .fileName(fileName)
            .fieldNames(columnName)
            .type(type)
            .lineNumber(lineNumber)
            .value(value)
            .params(param)
            .build());

    // File
    val tupleState = new TupleState(lineNumber);
    tupleState.reportError(type, columnName, value, param);
    writer.write(tupleState);
  }

  private static boolean isEGA(Term egaTerm, String rawDataRepository) {
    // Need to check code and value
    return rawDataRepository.equals(egaTerm.getValue()) || rawDataRepository.equals(egaTerm.getCode());
  }

  private static List<FileType> resolveMetaFileTypes(ValidationContext context) {
    return context.getDataTypes().stream()
        .filter(DataType::isFeatureType)
        .map(DataType::asFeatureType)
        .map(FeatureType::getMetaFileType)
        .collect(toImmutableList());
  }

  private static Term resolveEGATerm(ValidationContext context) {
    return CodeLists.getRawDataRepositoriesEGATerm(context.getCodeLists());
  }

  private static List<String> resolveFileAccessions(String rawDataAccession) {
    return COLON.splitToList(rawDataAccession).stream()
        .filter(value -> EGAAccessionType.from(value).orElse(null) == EGAAccessionType.FILE)
        .collect(toImmutableList());
  }

  private static TupleStateWriter createTupleStateWriter(ValidationContext context, Path file) throws IOException {
    return new TupleStateWriter(
        context.getFileSystem(), new Path(context.getSubmissionDirectory().getValidationDirPath()), file);
  }

  private static FileParser<Map<String, String>> createMetaFileParser(ValidationContext context,
      FileType metaFileType) {
    return newMapFileParser(context.getFileSystem(), context.getFileSchema(metaFileType));
  }

}