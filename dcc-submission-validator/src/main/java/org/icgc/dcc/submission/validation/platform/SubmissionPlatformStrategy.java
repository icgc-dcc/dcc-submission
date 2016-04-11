/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU License v3.0.
 * You should have received a copy of the GNU General License along with                                  
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
package org.icgc.dcc.submission.validation.platform;

import static org.icgc.dcc.common.core.util.Separators.DOUBLE_DASH;

import java.io.InputStream;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.core.dcc.DccConstants;
import org.icgc.dcc.common.core.util.Separators;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.primary.core.FlowType;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import cascading.flow.FlowConnector;
import cascading.tap.Tap;

public interface SubmissionPlatformStrategy {

  public static final String FIELD_SEPARATOR = DccConstants.INPUT_FILES_SEPARATOR;
  public static final Splitter FIELD_SPLITTER = Separators.getCorrespondingSplitter(FIELD_SEPARATOR);
  public static final Joiner FIELD_JOINER = Separators.getCorrespondingJoiner(FIELD_SEPARATOR);

  public static final String REPORT_FILES_INFO_SEPARATOR = DOUBLE_DASH;
  public static final Joiner REPORT_FILES_INFO_JOINER = Joiner.on(REPORT_FILES_INFO_SEPARATOR);

  FlowConnector getFlowConnector();

  /**
   * TODO: Adapt submission code to use {@link #getNormalizerSourceTap(FileSchema)} since we can now assume the header
   * is known (and therefore we should use TextDelimited rather than TextLine.
   */
  Tap<?, ?, ?> getSourceTap(String fileName);

  /**
   * See comment in {@link #getSourceTap(String)}.
   */
  Tap<?, ?, ?> getNormalizerSourceTap(String fileName);

  Tap<?, ?, ?> getReportTap(String fileName, FlowType type, String reportName);

  /**
   * Used to read back a report that was produced during the execution of a Flow. This does not use a Tap so that it can
   * be executed outside of a Flow.
   */
  InputStream readReportTap(String fileName, FlowType type, String reportName);

  /**
   * TODO
   */
  Path getFile(String fileName);

  /**
   * TODO: merge with {@link ValidationContext#getSsmPrimaryFiles()}?
   */
  List<String> listFileNames(String pattern);

}
