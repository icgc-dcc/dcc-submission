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
import static org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter.UNIQUE_REMAINING;
import static org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter.UNIQUE_START;
import static org.icgc.dcc.submission.validation.core.ErrorType.TOO_MANY_CONFIDENTIAL_OBSERVATIONS_ERROR;

import java.util.List;

import lombok.NonNull;
import lombok.Value;
import lombok.val;
import lombok.experimental.Builder;

import org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter;
import org.icgc.dcc.submission.normalization.NormalizationValidator.ConnectedCascade;
import org.icgc.dcc.submission.validation.core.ValidationContext;

import com.typesafe.config.Config;

/**
 * 
 * <p>
 * TODO: non-static + split internal/external report-related logic (or merge with {@link NormalizationReport})
 */
public class NormalizationReporter {

  private static final String TAB = "\t";
  private static final String NEWLINE = System.getProperty("line.separator");

  static final String INTERNAL_REPORT_MESSAGE = "Statistics for normalization:";

  /**
   * TODO
   * <p>
   * All of them for now.
   */
  private static List<NormalizationCounter> INTERNAL_REPORT_COUNTERS = newArrayList(NormalizationCounter.values());

  public static void performSanityChecks(ConnectedCascade connectedCascade) {
    long totalEnd = connectedCascade.getCounterValue(TOTAL_END);
    long totalStart = connectedCascade.getCounterValue(TOTAL_START);
    long masked = connectedCascade.getCounterValue(MASKED);
    long markedAsControlled = connectedCascade.getCounterValue(MARKED_AS_CONTROLLED);
    long dropped = connectedCascade.getCounterValue(DROPPED);
    long uniqueStart = connectedCascade.getCounterValue(UNIQUE_START);
    long uniqueFiltered = connectedCascade.getCounterValue(UNIQUE_REMAINING);

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
    sb.append(INTERNAL_REPORT_MESSAGE);
    sb.append(NEWLINE);
    for (val counter : INTERNAL_REPORT_COUNTERS) {
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
  public static NormalizationChecker collectPotentialErrors(
      Config config, ConnectedCascade connectedCascade, String fileName) {

    long markedAsControlled = connectedCascade.getCounterValue(MARKED_AS_CONTROLLED);
    long totalStart = connectedCascade.getCounterValue(TOTAL_START);
    float threshold = NormalizationConfig.getConfidentialErrorThreshold(config);

    return NormalizationChecker.builder()
        .fileName(fileName)
        .count(markedAsControlled)
        .total(totalStart)
        .threshold(threshold)
        .build();
  }

  /**
   * 
   */
  public static void reportError(ValidationContext validationContext, NormalizationChecker normalizationError) {
    validationContext
        .reportError(
            normalizationError.getFileName(),
            TOO_MANY_CONFIDENTIAL_OBSERVATIONS_ERROR,
            normalizationError.getCount(),
            normalizationError.getTotal(),
            normalizationError.getThreshold());
  }

  @Value
  @Builder
  static final class NormalizationChecker {

    @NonNull
    private final String fileName;
    private final long count;
    private final long total;
    private final float threshold;

    public boolean isLikelyErroneous() {
      return count > total * threshold;
    }
  }
}
