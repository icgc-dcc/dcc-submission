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

import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Iterables.filter;
import static java.lang.String.format;
import static org.icgc.dcc.submission.normalization.steps.RedundantObservationRemoval.ANALYSIS_ID_FIELD;

import java.util.Map.Entry;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;
import lombok.experimental.Builder;

import org.icgc.dcc.submission.normalization.NormalizationValidator.ConnectedCascade;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * 
 */
@Builder
@Value
public final class NormalizationReport {

  private static final boolean EXTERNAL = true;
  private static final boolean INTERNAL = false;

  private final String projectKey;
  private final ImmutableMap<NormalizationCounter, Long> counters;

  public ImmutableSet<Entry<NormalizationCounter, Long>> getExternalReportCounters() {
    return copyOf(filter(
        counters.entrySet(),
        new Predicate<Entry<NormalizationCounter, Long>>() {

          @Override
          public boolean apply(Entry<NormalizationCounter, Long> input) {
            NormalizationCounter counter = input.getKey();
            return counter.externalReport;
          }
        }));
  }

  @RequiredArgsConstructor
  public enum NormalizationCounter {
    TOTAL_START("TODO", EXTERNAL),
    TOTAL_END("TODO", EXTERNAL),
    UNIQUE_START(format("Number of unique '%s' before filtering", ANALYSIS_ID_FIELD), INTERNAL),
    DROPPED("Number of observations dropped due to redundancy", INTERNAL),
    UNIQUE_FILTERED(format("Number of unique '%s' remaining after filtering", ANALYSIS_ID_FIELD), INTERNAL),
    MARKED_AS_CONTROLLED("TODO", EXTERNAL),
    MASKED("TODO", EXTERNAL);

    public static final long COUNT_INCREMENT = 1;

    @Getter
    private final String displayName;

    /**
     * Whether the counter is to be used for external reporting or not.
     */
    private final boolean externalReport;

    static ImmutableMap<NormalizationCounter, Long> report(ConnectedCascade connected) {
      val counters = new ImmutableMap.Builder<NormalizationCounter, Long>();
      for (val counter : values()) {
        counters.put(
            counter,
            connected.getCounterValue(counter));
      }
      return counters.build();
    }
  }
}
