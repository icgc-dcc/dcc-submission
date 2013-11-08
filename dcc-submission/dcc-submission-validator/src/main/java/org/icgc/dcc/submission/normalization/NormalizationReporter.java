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

import static com.google.common.collect.Lists.newArrayList;
import lombok.val;

import org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter;
import org.icgc.dcc.submission.normalization.NormalizationValidator.ConnectedCascade;

/**
 * 
 */
public class NormalizationReporter {

  private static final String TAB = "\t";
  private static final String NEWLINE = System.getProperty("line.separator");

  static final String MESSAGE = "Statistics for the dropping of observations:";

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
}
