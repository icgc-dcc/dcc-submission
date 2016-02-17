/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.platform;

import static org.icgc.dcc.common.hadoop.fs.FileSystems.getDefaultLocalFileSystem;

import java.io.InputStream;
import java.util.Map;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.common.cascading.CascadingContext;
import org.icgc.dcc.submission.validation.primary.core.FlowType;

@Slf4j
public class LocalSubmissionPlatformStrategy extends BaseSubmissionPlatformStrategy {

  public LocalSubmissionPlatformStrategy(
      @NonNull final Map<String, String> hadoopProperties,
      @NonNull final Path source,
      @NonNull final Path output) {
    super(hadoopProperties, getDefaultLocalFileSystem(), source, output);
  }

  @Override
  protected CascadingContext getCascadingContext() {
    return CascadingContext.getLocal();
  }

  @Override
  protected Map<?, ?> augmentFlowProperties(@NonNull final Map<?, ?> properties) {
    return properties; // Nothing to add in local mode
  }

  @Override
  @SneakyThrows
  public InputStream readReportTap(String fileName, FlowType type, String reportName) {
    val reportPath = getReportPath(fileName, type, reportName);
    log.info("Streaming through report: '{}'", reportPath);
    return fileSystem.open(reportPath);
  }

}
