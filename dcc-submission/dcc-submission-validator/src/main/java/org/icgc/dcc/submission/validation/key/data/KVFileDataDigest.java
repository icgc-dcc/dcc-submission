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
package org.icgc.dcc.submission.validation.key.data;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Sets.newTreeSet;
import static lombok.AccessLevel.PROTECTED;
import static org.icgc.dcc.submission.core.parser.FileParsers.newListFileParser;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_M_FKS1;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_M_FKS2;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_P_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.CNSM_S_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.DONOR_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.EXP_G_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.EXP_M_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.EXP_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.JCN_M_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.JCN_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.JCN_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.MAPPER;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.METH_M_FKS1;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.METH_M_FKS2;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.METH_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.METH_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.METH_P_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.METH_S_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.MIRNA_M_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.MIRNA_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.MIRNA_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.MIRNA_S_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.MIRNA_S_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.PEXP_M_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.PEXP_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.PEXP_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SAMPLE_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SAMPLE_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SGV_M_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SGV_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SGV_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SPECIMEN_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SPECIMEN_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SSM_M_FKS1;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SSM_M_FKS2;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SSM_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.SSM_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.STSM_M_FKS1;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.STSM_M_FKS2;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.STSM_M_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.STSM_P_FKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.STSM_P_PKS;
import static org.icgc.dcc.submission.validation.key.core.KVConstants.STSM_S_FKS;
import static org.icgc.dcc.submission.validation.key.data.KVKeyValues.NOT_APPLICABLE;
import static org.icgc.dcc.submission.validation.key.data.KVKeyValues.from;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.CNSM_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.DONOR;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_G;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.EXP_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.JCN_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.JCN_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.METH_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.MIRNA_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.MIRNA_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.MIRNA_S;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.PEXP_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SAMPLE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SGV_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SGV_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SPECIMEN;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.SSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.STSM_M;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.STSM_P;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.STSM_S;

import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.parser.FileRecordProcessor;
import org.icgc.dcc.submission.validation.key.core.KVFileDescription;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;

/**
 * Represents the relevant data for a given file (keys mostly).
 * <p>
 * Not abstract because of the "empty" instance.
 */
@Slf4j
@RequiredArgsConstructor(access = PROTECTED)
public class KVFileDataDigest {

  /**
   * TODO: temporarily...
   */
  public static final boolean TUPLE_CHECKS_ENABLED = true;

  @Getter
  protected final KVFileDescription kvFileDescription;
  private final long logThreshold;

  @Getter
  protected final Set<KVKeyValues> pks = newTreeSet(); // TODO: change to arrays?

  public static KVFileDataDigest getEmptyInstance(@NonNull KVFileDescription kvFileDescription) {
    return new KVFileDataDigest(kvFileDescription, -1); // -1: no need for a threshold
  }

  @SneakyThrows
  public KVFileDataDigest processFile() {
    log.info("{}", kvFileDescription);

    val parser = newListFileParser();
    checkState(!kvFileDescription.isPlaceholder(), "TODO");
    parser.parse(kvFileDescription.getDataFilePath().get(), new FileRecordProcessor<List<String>>() {

      @Override
      public void process(long lineNumber, List<String> record) {
        val tuple = getTuple(kvFileDescription.getFileType(), record);
        log.debug("tuple: '{}'", tuple);

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
    checkState(kvFileDescription.getSubmissionType().isExistingData()); // incremental MUST overide it
  }

  protected void updatePksIfApplicable(KVTuple tuple) {
    if (tuple.hasPk()) {
      pks.add(tuple.getPk());
    } else {
      if (TUPLE_CHECKS_ENABLED) checkState(!kvFileDescription.getFileType().hasPk(), "TODO");
    }
  }

  /**
   * TODO: encode in dictionary data structure rather (hardcoded elsewhere, at least the PKs)
   */
  protected KVTuple getTuple(KVFileType fileType, List<String> row) {
    KVKeyValues pk = null, fk1 = null, fk2 = null;

    // Clinical
    if (fileType == DONOR) {
      pk = from(row, DONOR_PKS);
      fk1 = NOT_APPLICABLE;
      fk2 = NOT_APPLICABLE;
    } else if (fileType == SPECIMEN) {
      pk = from(row, SPECIMEN_PKS);
      fk1 = from(row, SPECIMEN_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == SAMPLE) {
      pk = from(row, SAMPLE_PKS);
      fk1 = from(row, SAMPLE_FKS);
      fk2 = NOT_APPLICABLE;
    }

    // Ssm
    else if (fileType == SSM_M) {
      pk = from(row, SSM_M_PKS);
      fk1 = from(row, SSM_M_FKS1);
      fk2 = from(row, SSM_M_FKS2); // TODO: handle case where value is null or a missing code
    } else if (fileType == SSM_P) {
      pk = NOT_APPLICABLE;
      fk1 = from(row, SSM_P_FKS);
      fk2 = NOT_APPLICABLE;
    }

    // Cnsm
    else if (fileType == CNSM_M) {
      pk = from(row, CNSM_M_PKS);
      fk1 = from(row, CNSM_M_FKS1);
      fk2 = from(row, CNSM_M_FKS2);
    } else if (fileType == CNSM_P) {
      pk = from(row, CNSM_P_PKS);
      fk1 = from(row, CNSM_P_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == CNSM_S) {
      pk = NOT_APPLICABLE;
      fk1 = from(row, CNSM_S_FKS);
      fk2 = NOT_APPLICABLE;
    }

    // Stsm
    else if (fileType == STSM_M) {
      pk = from(row, STSM_M_PKS);
      fk1 = from(row, STSM_M_FKS1);
      fk2 = from(row, STSM_M_FKS2);
    } else if (fileType == STSM_P) {
      pk = from(row, STSM_P_PKS);
      fk1 = from(row, STSM_P_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == STSM_S) {
      pk = NOT_APPLICABLE;
      fk1 = from(row, STSM_S_FKS);
      fk2 = NOT_APPLICABLE;
    }

    // Mirna
    else if (fileType == MIRNA_M) {
      pk = from(row, MIRNA_M_PKS);
      fk1 = from(row, MIRNA_M_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == MIRNA_P) {
      pk = NOT_APPLICABLE; // Special case: uniqueness is not enforced
      fk1 = from(row, MIRNA_P_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == MIRNA_S) {
      pk = from(row, MIRNA_S_PKS);
      fk1 = from(row, MIRNA_S_FKS);
      fk2 = NOT_APPLICABLE;
    }

    // Meth
    else if (fileType == METH_M) {
      pk = from(row, METH_M_PKS);
      fk1 = from(row, METH_M_FKS1);
      fk1 = from(row, METH_M_FKS2);
    } else if (fileType == METH_P) {
      pk = from(row, METH_P_PKS);
      fk1 = from(row, METH_P_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == METH_S) {
      pk = NOT_APPLICABLE;
      fk1 = from(row, METH_S_FKS);
      fk2 = NOT_APPLICABLE;
    }

    // Exp
    else if (fileType == EXP_M) {
      pk = from(row, EXP_M_PKS);
      fk1 = from(row, EXP_M_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == EXP_G) {
      pk = NOT_APPLICABLE;
      fk1 = from(row, EXP_G_FKS);
      fk2 = NOT_APPLICABLE;
    }

    // Pexp
    else if (fileType == SSM_M) {
      pk = from(row, PEXP_M_PKS);
      fk1 = from(row, PEXP_M_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == PEXP_P) {
      pk = NOT_APPLICABLE;
      fk1 = from(row, PEXP_P_FKS);
      fk2 = NOT_APPLICABLE;
    }

    // Jcn
    else if (fileType == JCN_M) {
      pk = from(row, JCN_M_PKS);
      fk1 = from(row, JCN_M_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == JCN_P) {
      pk = NOT_APPLICABLE;
      fk1 = from(row, JCN_P_FKS);
      fk2 = NOT_APPLICABLE;
    }

    // Sgv
    else if (fileType == SGV_M) {
      pk = from(row, SGV_M_PKS);
      fk1 = from(row, SGV_M_FKS);
      fk2 = NOT_APPLICABLE;
    } else if (fileType == SGV_P) {
      pk = NOT_APPLICABLE;
      fk1 = from(row, SGV_P_FKS);
      fk2 = NOT_APPLICABLE;
    }

    if (TUPLE_CHECKS_ENABLED) checkState(pk != null || fk1 != null, "TODO: '%s'", row);
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