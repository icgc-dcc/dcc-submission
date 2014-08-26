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

import static lombok.AccessLevel.PRIVATE;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.mapred.TaskAttemptID;
import org.apache.hadoop.mapred.TaskCompletionEvent;
import org.apache.hadoop.mapred.TaskLog;

import cascading.flow.Flow;
import cascading.stats.CascadingStats.Status;
import cascading.stats.hadoop.HadoopSliceStats.HadoopAttempt;
import cascading.stats.hadoop.HadoopStepStats;

/**
 * Utility class for reporting on flow failures
 */
@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class FlowExecutorReporter {

  public static void reportFailure(Flow<?> flow) {
    log.info("Handling exception for flow: {}", flow);
    try {
      for (val stepStats : flow.getFlowStats().getFlowStepStats()) {
        log.info("*  Step stats: {}", stepStats);

        val hadoop = HadoopStepStats.class.isAssignableFrom(stepStats.getClass());
        if (hadoop) {
          val hadoopStepStats = (HadoopStepStats) stepStats;
          for (val taskStats : hadoopStepStats.getTaskStats().values()) {
            log.info("*  Task stats: {}", taskStats);
            for (val hadoopAttempt : taskStats.getAttempts().values()) {
              log.info("*    Hadoop attempt: {}", hadoopAttempt);
              if (hadoopAttempt.getStatusFor() == Status.FAILED) {
                // TODO: Actually fetch the log
                log.info("*      Attempt log url: {}", getAttemptLogUrl(hadoopAttempt));
              }
            }
          }
        }
      }
    } catch (Throwable t) {
      log.error("Error reporting flow failure: ", t);
    }
  }

  private static String getAttemptLogUrl(HadoopAttempt hadoopAttempt) {
    val attemptId = getAttemptId(hadoopAttempt);

    return new StringBuilder(hadoopAttempt.getTaskTrackerHttp())
        .append("/tasklog?attemptid=")
        .append(attemptId)
        .append("&plaintext=true")
        .append("&filter=")
        .append(TaskLog.LogName.STDOUT)
        .toString();
  }

  /**
   * Workaround to get the Hadoop attempt id from Cascading's abstraction.
   * 
   * @see https://groups.google.com/forum/#!topic/cascading-user/OQm5lAwsw7o
   */
  @SneakyThrows
  private static TaskAttemptID getAttemptId(HadoopAttempt hadoopAttempt) {
    val eventField = hadoopAttempt.getClass().getDeclaredField("event");
    eventField.setAccessible(true);

    val event = (TaskCompletionEvent) eventField.get(hadoopAttempt);
    return event.getTaskAttemptId();
  }

}
