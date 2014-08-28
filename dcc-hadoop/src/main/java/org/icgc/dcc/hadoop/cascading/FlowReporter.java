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
package org.icgc.dcc.hadoop.cascading;

import static com.google.common.base.Charsets.UTF_8;
import static com.google.common.io.Resources.readLines;
import static lombok.AccessLevel.PRIVATE;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.mapred.TaskAttemptID;
import org.apache.hadoop.mapred.TaskCompletionEvent;
import org.apache.hadoop.mapred.TaskLog;

import cascading.flow.Flow;
import cascading.stats.CascadingStats.Status;
import cascading.stats.FlowStepStats;
import cascading.stats.hadoop.HadoopSliceStats;
import cascading.stats.hadoop.HadoopSliceStats.HadoopAttempt;
import cascading.stats.hadoop.HadoopStepStats;

import com.google.common.io.LineProcessor;

/**
 * Utility class for reporting on flow failures
 */
@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class FlowReporter {

  public static void reportFlowFailure(Flow<?> flow) {
    log.info("Reporting failure for flow: {}", flow);

    try {
      for (val flowStepStats : getFlowStepStats(flow)) {
        reportFlowStepFailure(flowStepStats);
      }
    } catch (Throwable t) {
      log.error("Error reporting flow failure: ", t);
    }
  }

  private static void reportFlowStepFailure(FlowStepStats flowStepStats) {
    log.info("*  Flow step stats {}: {}", flowStepStats.getClass().getName(), flowStepStats);

    val local = !isHadoopStepStats(flowStepStats);
    if (local) {
      log.info("*    (Local has no reporting)");
      return;
    }

    for (val hadoopSliceStats : getHadoopSliceStats(flowStepStats)) {
      log.info("*    Hadoop slice stats: {}", hadoopSliceStats);

      for (val hadoopAttempt : getHadoopAttempts(hadoopSliceStats)) {
        log.info("*      Hadoop attempt: {}", hadoopAttempt);

        val failed = isFailedHadoopAttempt(hadoopAttempt);
        if (failed) {
          log.info("*        Failed Hadoop attempt log URL: {}", getHadoopAttemptLogUrl(hadoopAttempt));

          // Report the failed attempt
          reportHadoopAttempt(hadoopAttempt);
        }
      }
    }
  }

  @SneakyThrows
  private static void reportHadoopAttempt(HadoopSliceStats.HadoopAttempt hadoopAttempt) {
    val url = getHadoopAttemptLogUrl(hadoopAttempt);

    readLines(new URL(url), UTF_8, new LineProcessor<Void>() {

      @Override
      public boolean processLine(String line) throws IOException {
        // Merge into client side long
        log.error(line);

        return true;
      }

      @Override
      public Void getResult() {
        return null;
      }

    });
  }

  private static List<FlowStepStats> getFlowStepStats(Flow<?> flow) {
    return flow.getFlowStats().getFlowStepStats();
  }

  private static boolean isHadoopStepStats(FlowStepStats instance) {
    val superClass = HadoopStepStats.class;
    val subClass = instance.getClass();

    // TODO: Only the first should be needed here...
    return superClass.isAssignableFrom(subClass) || subClass.isAssignableFrom(superClass);
  }

  private static Collection<HadoopAttempt> getHadoopAttempts(HadoopSliceStats taskStats) {
    return taskStats.getAttempts().values();
  }

  private static Collection<HadoopSliceStats> getHadoopSliceStats(FlowStepStats stepStats) {
    return ((HadoopStepStats) stepStats).getTaskStats().values();
  }

  private static boolean isFailedHadoopAttempt(HadoopSliceStats.HadoopAttempt hadoopAttempt) {
    return hadoopAttempt.getStatusFor() == Status.FAILED;
  }

  private static String getHadoopAttemptLogUrl(HadoopAttempt hadoopAttempt) {
    return new StringBuilder(hadoopAttempt.getTaskTrackerHttp())
        .append("/tasklog?attemptid=")
        .append(getHadoopAttemptId(hadoopAttempt))
        .append("&plaintext=true")
        .append("&filter=")
        .append(TaskLog.LogName.STDOUT)
        .toString();
  }

  @SneakyThrows
  private static TaskAttemptID getHadoopAttemptId(HadoopAttempt hadoopAttempt) {
    // https://groups.google.com/forum/#!topic/cascading-user/OQm5lAwsw7o
    val eventField = hadoopAttempt.getClass().getDeclaredField("event");
    eventField.setAccessible(true);

    val event = (TaskCompletionEvent) eventField.get(hadoopAttempt);
    return event.getTaskAttemptId();
  }

}
