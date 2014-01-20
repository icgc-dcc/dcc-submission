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
package org.icgc.dcc.submission.validation.key.core;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Collection;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.SubmissionDataType;
import org.icgc.dcc.submission.core.parser.FileLineListParser;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.validation.key.report.KVReport;

import cascading.flow.hadoop.HadoopFlowStep;

/**
 * Runner that operates within a Cascading step.
 */
@Slf4j
@Value
public class KVValidatorRunner implements Runnable, Serializable {

  /**
   * Fields that need to be {@link Serializable} to survive the trip to the cluster.
   * 
   * @see {@link HadoopFlowStep#pack()}
   */
  @NonNull
  private final URI fsUri;
  @NonNull
  private final Collection<SubmissionDataType> dataTypes;
  @NonNull
  private final Dictionary dictionary;
  @NonNull
  private final String submissionPath;
  @NonNull
  private final String reportPath;

  @Override
  @SneakyThrows
  public void run() {
    try {
      validate();
    } catch (Throwable t) {
      log.error("Error performing key validation:", t);
      throw t;
    }
  }

  private void validate() throws IOException {
    val fileSystem = getFileSystem();
    val report = new KVReport(fileSystem, new Path(reportPath));
    try {
      val validator = new KVValidator(
          new KVFileParser(fileSystem, new FileLineListParser(), false),
          new KVFileSystem(fileSystem, dataTypes, dictionary, new Path(submissionPath)), report);

      log.info("Starting key validation...");
      validator.processSubmission();
      log.info("Finished key validation");
    } finally {
      report.close();
    }
  }

  /**
   * Re-establishes the file system cluster-side.
   */
  @SneakyThrows
  private FileSystem getFileSystem() {
    return FileSystem.get(fsUri, new Configuration());
  }

}