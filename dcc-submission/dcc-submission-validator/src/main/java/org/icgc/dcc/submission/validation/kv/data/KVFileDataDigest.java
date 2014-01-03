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
package org.icgc.dcc.submission.validation.kv.data;

import static com.google.common.base.Preconditions.checkState;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.lang.StringUtils.repeat;
import static org.icgc.dcc.submission.core.parser.FileParsers.newListFileParser;
import static org.icgc.dcc.submission.validation.kv.KVConstants.CNSM_M_FKS1;
import static org.icgc.dcc.submission.validation.kv.KVConstants.CNSM_M_FKS2;
import static org.icgc.dcc.submission.validation.kv.KVConstants.CNSM_M_PKS;
import static org.icgc.dcc.submission.validation.kv.KVConstants.CNSM_P_FKS;
import static org.icgc.dcc.submission.validation.kv.KVConstants.CNSM_P_PKS;
import static org.icgc.dcc.submission.validation.kv.KVConstants.CNSM_S_FKS;
import static org.icgc.dcc.submission.validation.kv.KVConstants.DONOR_PKS;
import static org.icgc.dcc.submission.validation.kv.KVConstants.SAMPLE_FKS;
import static org.icgc.dcc.submission.validation.kv.KVConstants.SAMPLE_PKS;
import static org.icgc.dcc.submission.validation.kv.KVConstants.SPECIMEN_FKS;
import static org.icgc.dcc.submission.validation.kv.KVConstants.SPECIMEN_PKS;
import static org.icgc.dcc.submission.validation.kv.KVConstants.SSM_M_FKS1;
import static org.icgc.dcc.submission.validation.kv.KVConstants.SSM_M_FKS2;
import static org.icgc.dcc.submission.validation.kv.KVConstants.SSM_M_PKS;
import static org.icgc.dcc.submission.validation.kv.KVConstants.SSM_P_FKS;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.CNSM_P;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.CNSM_S;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.SSM_M;
import static org.icgc.dcc.submission.validation.kv.enumeration.KVFileType.SSM_P;

import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.core.parser.FileRecordProcessor;
import org.icgc.dcc.submission.validation.kv.enumeration.KVFileType;
import org.icgc.dcc.submission.validation.kv.enumeration.KVSubmissionType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;

/**
 * Represents the relevant data for a given file (keys mostly).
 * <p>
 * Not abstract because of the "empty" instance.
 */
@Slf4j
@RequiredArgsConstructor(access = PRIVATE)
public class KVFileDataDigest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  // TODO: encapsulate in other object?
  @NonNull
  protected final KVSubmissionType submissionType;
  @NonNull
  protected final KVFileType fileType;
  private final String path; // TODO: optional?
  private final boolean placeholder;
  private final long logThreshold;

  @Getter
  protected final Set<KVKeyValues> pks = Sets.<KVKeyValues> newTreeSet(); // TODO: change to arrays?

  public static KVFileDataDigest getEmptyInstance(@NonNull KVSubmissionType submissionType, @NonNull KVFileType fileType) {
    return new KVFileDataDigest(
        submissionType,
        fileType,
        (String) null, // no file path
        true, // this IS a placeholder
        -1); // No need for a threshold
  }

  protected KVFileDataDigest(
      @NonNull KVSubmissionType submissionType,
      @NonNull KVFileType fileType,
      @NonNull String path,
      long logThreshold) {
    this(submissionType,
        fileType,
        path,
        false,
        logThreshold); // this is NOT a placeholder
  }

  @SneakyThrows
  public KVFileDataDigest processFile() {
    checkState(!placeholder);
    log.info("{}", repeat("=", 75));
    log.info("{}", Joiner.on(", ").join(submissionType, fileType, path));

    val parser = newListFileParser();
    parser.parse(new Path(path), new FileRecordProcessor<List<String>>() {

      @Override
      public void process(long lineNumber, List<String> record) {
        val tuple = getTuple(fileType, record);
        log.debug("tuple: {}", tuple);

        processTuple(tuple, lineNumber);

        if ((lineNumber % logThreshold) == 0) {
          logProcessedLine(lineNumber, false);
        }
      }

    });

    postProcessing();

    return this;
  }

  /**
   * TODO: include lineCount in tuple?
   */
  protected void processTuple(KVTuple tuple, long lineCount) {
    checkState(false); // TODO: explain
  }

  /**
   * For surjection checks in the case of incremental data (nothing to do for existing data).
   */
  protected void postProcessing() {
    checkState(submissionType.isExistingData()); // incremental MUST overide it
  }

  protected KVTuple getTuple(KVFileType fileType, List<String> row) {
    KVKeyValues pk = null, fk1 = null, fk2 = null;

    // Clinical
    if (fileType == DONOR) {
      pk = KVKeyValues.from(row, DONOR_PKS);
      fk1 = KVKeyValues.NOT_APPLICABLE;
      fk2 = KVKeyValues.NOT_APPLICABLE;
    } else if (fileType == SPECIMEN) {
      pk = KVKeyValues.from(row, SPECIMEN_PKS);
      fk1 = KVKeyValues.from(row, SPECIMEN_FKS);
      fk2 = KVKeyValues.NOT_APPLICABLE;
    } else if (fileType == SAMPLE) {
      pk = KVKeyValues.from(row, SAMPLE_PKS);
      fk1 = KVKeyValues.from(row, SAMPLE_FKS);
      fk2 = KVKeyValues.NOT_APPLICABLE;
    }

    // Ssm
    else if (fileType == SSM_M) {
      pk = KVKeyValues.from(row, SSM_M_PKS);
      fk1 = KVKeyValues.from(row, SSM_M_FKS1);
      fk2 = KVKeyValues.from(row, SSM_M_FKS2); // TODO: handle case where value is null or a missing code
    } else if (fileType == SSM_P) {
      pk = KVKeyValues.NOT_APPLICABLE;
      fk1 = KVKeyValues.from(row, SSM_P_FKS);
      fk2 = KVKeyValues.NOT_APPLICABLE;
    }

    // Cnsm
    else if (fileType == CNSM_M) {
      pk = KVKeyValues.from(row, CNSM_M_PKS);
      fk1 = KVKeyValues.from(row, CNSM_M_FKS1);
      fk2 = KVKeyValues.from(row, CNSM_M_FKS2); // TODO: handle case where value is null or a missing code
    } else if (fileType == CNSM_P) {
      pk = KVKeyValues.from(row, CNSM_P_PKS);
      fk1 = KVKeyValues.from(row, CNSM_P_FKS);
      fk2 = KVKeyValues.NOT_APPLICABLE;
    } else if (fileType == CNSM_S) {
      pk = KVKeyValues.NOT_APPLICABLE;
      fk1 = KVKeyValues.from(row, CNSM_S_FKS);
      fk2 = KVKeyValues.NOT_APPLICABLE;
    }

    checkState(pk != null || fk1 != null, "TODO: '%s'", row);
    return new KVTuple(pk, fk1, fk2);
  }

  public boolean pksContains(
      @NonNull// TODO: consider removing such time consuming checks?
      KVKeyValues keys) {
    return pks.contains(keys);
  }

  protected void logProcessedLine(long lineCount, boolean finished) {
    log.info("'{}' lines processed" + (finished ? " (finished)" : ""), lineCount);
  }

  @Override
  public String toString() {
    return toJsonSummaryString();
  }

  @SneakyThrows
  public String toJsonSummaryString() {
    return "\n" + MAPPER
        .writerWithDefaultPrettyPrinter()
        .writeValueAsString(this); // TODO: show sample only (first and last 10 for instance) + excluding nulls
  }
}