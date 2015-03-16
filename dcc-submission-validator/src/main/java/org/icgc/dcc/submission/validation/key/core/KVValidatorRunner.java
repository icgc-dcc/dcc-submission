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

import static org.icgc.dcc.common.core.util.FormatUtils.formatMemory;

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
import org.icgc.dcc.common.cascading.FlowExecutorJob;
import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.common.hadoop.parser.FileLineListParser;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.validation.key.report.KVReporter;

import cascading.flow.hadoop.HadoopFlowStep;

import com.google.common.base.Stopwatch;

/**
 * Runner that operates within a Cascading step.
 */
@Slf4j
@Value
public class KVValidatorRunner implements FlowExecutorJob, Serializable {

  /**
   * Fields that need to be {@link Serializable} to survive the trip to the cluster.
   * 
   * @see {@link HadoopFlowStep#pack()}
   */
  @NonNull
  private final URI fsUri;
  @NonNull
  private final Collection<DataType> dataTypes;
  @NonNull
  private final Dictionary dictionary;
  @NonNull
  private final String submissionPath;
  @NonNull
  private final String systemPath;
  @NonNull
  private final String reportPath;

  @Override
  @SneakyThrows
  public void execute(@NonNull Configuration configuration) {
    try {
      validate(configuration);
    } catch (Throwable t) {
      log.error("Error performing key validation:", t);
      throw t;
    }
  }

  private void validate(Configuration configuration) throws IOException {
    log.info("Starting key validation with memory: {}...", formatMemory());

    val fileSystem = getFileSystem(configuration);
    val kvDictionary = new KVHardcodedDictionary(); // TODO: inject
    val report = new KVReporter(kvDictionary, fileSystem, new Path(reportPath));
    val watch = createStopwatch();
    try {
      val validator = new KVSubmissionProcessor(
          kvDictionary,
          new KVFileParser(fileSystem, new FileLineListParser(), false),
          new KVFileSystem(fileSystem, dataTypes, dictionary.getPatterns(),
              new Path(submissionPath), new Path(systemPath)), report);

      log.info("Processing submission...");
      validator.processSubmission();
      log.info("Finished processing submission...");
    } finally {
      report.close();
      log.info("Finished key validation in {}", watch);
    }
  }

  @SneakyThrows
  private FileSystem getFileSystem(Configuration configuration) {
    return FileSystem.get(fsUri, configuration);
  }

  @SuppressWarnings("deprecation")
  private static Stopwatch createStopwatch() {
    // Can't use the new API here because Hadoop doesn't know about it cluster side. Trying to use it will result in
    // errors.
    return new Stopwatch().start();
  }

}