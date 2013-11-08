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
package org.icgc.dcc.submission.normalization;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter.DROPPED;
import static org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter.MARKED_AS_CONTROLLED;
import static org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter.MASKED;
import static org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter.TOTAL_END;
import static org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter.TOTAL_START;
import static org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter.UNIQUE_FILTERED;
import static org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter.UNIQUE_START;
import static org.icgc.dcc.submission.validation.core.ErrorType.TOO_MANY_CONFIDENTIAL_OBSERVATIONS_ERROR;
import lombok.NonNull;
import lombok.Value;
import lombok.val;
import lombok.experimental.Builder;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter;
import org.icgc.dcc.submission.normalization.NormalizationValidator.ConnectedCascade;
import org.icgc.dcc.submission.validation.core.ValidationContext;

import com.google.common.base.Optional;

/**
 * 
 */
@Slf4j
public class NormalizationReporter {

  private static final String TAB = "\t";
  private static final String NEWLINE = System.getProperty("line.separator");

  static final String MESSAGE = "Statistics for the dropping of observations:";

  public static void performSanityChecks(ConnectedCascade connectedCascade) {
    long totalEnd = connectedCascade.getCounterValue(TOTAL_END);
    long totalStart = connectedCascade.getCounterValue(TOTAL_START);
    long masked = connectedCascade.getCounterValue(MASKED);
    long markedAsControlled = connectedCascade.getCounterValue(MARKED_AS_CONTROLLED);
    long dropped = connectedCascade.getCounterValue(DROPPED);
    long uniqueStart = connectedCascade.getCounterValue(UNIQUE_START);
    long uniqueFiltered = connectedCascade.getCounterValue(UNIQUE_FILTERED);

    checkState(
        totalEnd == (totalStart + masked - dropped),
        "Invalid counts encoutered: %s != (%s + %s - %s)",
        totalEnd, totalStart, masked, dropped);
    checkState(
        masked <= markedAsControlled,
        "Invalid counts encoutered: %s > %s",
        masked, markedAsControlled);
    checkState(
        uniqueStart <= totalStart,
        "Invalid counts encoutered: %s > %s",
        uniqueStart, totalStart);
    checkState(
        uniqueFiltered <= uniqueStart,
        "Invalid counts encoutered: %s > %s",
        uniqueFiltered, uniqueStart);
  }

  /**
   * 
   */
  public static String createInternalReportContent(ConnectedCascade connectedCascade) {
    val sb = new StringBuilder();
    sb.append(MESSAGE);
    sb.append(NEWLINE);
    for (val counter : newArrayList(
        NormalizationCounter.DROPPED,
        NormalizationCounter.UNIQUE_FILTERED)) {
      long counterValue = connectedCascade.getCounterValue(counter);
      sb.append(counterValue);
      sb.append(TAB);
      sb.append(counter.getDisplayName());
      sb.append(NEWLINE);
    }
    return sb.toString();
  }

  /**
   * 
   */
  public static Optional<NormalizationError> collectPotentialErrors(
      ConnectedCascade connectedCascade,
      String fileName) {
    long markedAsControlled = connectedCascade.getCounterValue(MARKED_AS_CONTROLLED);
    long totalStart = connectedCascade.getCounterValue(TOTAL_START);
    if (NormalizationError.isLikelyErroneous(
        markedAsControlled,
        totalStart)) {
      log.info("here");
      return Optional.of(
          NormalizationError.builder()
              .fileName(fileName)
              .count(markedAsControlled)
              .total(totalStart)
              .build());
    } else {
      log.info("");
      return Optional.<NormalizationError> absent();
    }
  }

  /**
   * 
   */
  public static void reportError(ValidationContext validationContext, NormalizationError normalizationError) {
    validationContext
        .reportError(
            normalizationError.getFileName(),
            TOO_MANY_CONFIDENTIAL_OBSERVATIONS_ERROR,
            normalizationError.getCount(),
            normalizationError.getTotal());
  }

  @Value
  @Builder
  static final class NormalizationError {

    private final static float THRESHOLD = 0.50f;

    @NonNull
    private final String fileName;
    private final long count;
    private final long total;

    private static boolean isLikelyErroneous(long count, long total) {
      return count > total * THRESHOLD;
    }
  }
}
