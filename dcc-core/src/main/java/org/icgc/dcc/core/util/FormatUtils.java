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
package org.icgc.dcc.core.util;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static lombok.AccessLevel.PRIVATE;
import static org.joda.time.Duration.standardSeconds;
import lombok.NoArgsConstructor;

import org.joda.time.Duration;
import org.joda.time.Period;

import com.google.common.base.Stopwatch;

@NoArgsConstructor(access = PRIVATE)
public final class FormatUtils {

  public static String formatBytes(long bytes) {
    return formatBytes(bytes, true);
  }

  public static String formatBytes(long bytes, boolean si) {
    int unit = si ? 1000 : 1024;
    if(bytes < unit) return bytes + " B";

    int exp = (int) (Math.log(bytes) / Math.log(unit));
    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");

    return format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }

  public static String formatCount(int count) {
    return format("%,d", count);
  }

  public static String formatCount(long count) {
    return format("%,d", count);
  }

  public static String formatPercent(float percent) {
    return format("%.2f", percent);
  }

  public static String formatDuration(long seconds) {
    Duration duration = standardSeconds(seconds);

    return format("%02d:%02d:%02d (hh:mm:ss)", //
        duration.getStandardHours(), duration.getStandardMinutes(), duration.getStandardSeconds());
  }

  public static String formatDuration(Stopwatch watch) {
    return formatDuration(watch.elapsedTime(SECONDS));
  }

  public static String formatPeriod(Period period) {
    period = period.normalizedStandard();

    return format("%02d:%02d:%02d (hh:mm:ss)",//
        period.getHours(), period.getMinutes(), period.getSeconds());
  }

}
