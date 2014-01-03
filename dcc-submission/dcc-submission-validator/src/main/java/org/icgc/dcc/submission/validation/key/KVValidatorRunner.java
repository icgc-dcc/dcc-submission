/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.validation.key;

import java.io.IOException;
import java.io.Serializable;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.validation.key.report.KVReport;

@RequiredArgsConstructor
@Slf4j
public class KVValidatorRunner implements Runnable, Serializable {

  private final long logThreshold;
  private final String reportPath;

  @Override
  public void run() {
    try {
      val report = createReport();
      try {
        val validator = createValidator(report);

        log.info("Starting key validation...");
        validator.validate();
        log.info("Finished key validation");
      } finally {
        report.close();
      }
    } catch (Throwable t) {
      log.error("Error performing key validation:", t);
    }
  }

  private KVValidator createValidator(KVReport report) {
    val validator = new KVValidator(report, logThreshold);
    return validator;
  }

  private KVReport createReport() throws IOException {
    val fileSystem = getDefaultFileSystem();
    val path = new Path(reportPath);
    val report = new KVReport(fileSystem, path);

    return report;
  }

  @SneakyThrows
  private static FileSystem getDefaultFileSystem() {
    // TODO: Resolve dynamically from context
    return FileSystem.getLocal(new Configuration());
  }

}