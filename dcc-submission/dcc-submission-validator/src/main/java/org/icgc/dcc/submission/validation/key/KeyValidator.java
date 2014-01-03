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
package org.icgc.dcc.submission.validation.key;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.codehaus.jackson.map.MappingIterator;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.submission.validation.cascading.CascadeExecutor;
import org.icgc.dcc.submission.validation.core.ValidationContext;
import org.icgc.dcc.submission.validation.core.Validator;
import org.icgc.dcc.submission.validation.key.error.KVError;

@RequiredArgsConstructor
@Slf4j
public class KeyValidator implements Validator {

  public static final String COMPONENT_NAME = "Key Validator";

  private final long logThreshold;

  @Override
  public String getName() {
    return COMPONENT_NAME;
  }

  @Override
  public void validate(ValidationContext context) throws InterruptedException {
    // TODO: Get from context
    val reportPath = "/tmp/report.json";
    val executor = new CascadeExecutor(context.getPlatformStrategy());
    val runnable = new KVValidatorRunner(logThreshold, reportPath);

    log.info("Starting key validation...");
    executor.execute(runnable);
    log.info("Finished key validation");

    log.info("Starting key validation report collection...");
    report(context, reportPath);
    log.info("Finished key validation report collection");
  }

  @SneakyThrows
  private void report(ValidationContext context, String reportPath) {
    @Cleanup
    val inputStream = createInputStream(context.getFileSystem(), new Path(reportPath));
    val errors = getErrors(inputStream);

    while (errors.hasNext()) {
      val error = errors.next();
      context.reportError(error.getFileName(), error.getType());
    }
  }

  private DataInputStream createInputStream(FileSystem fileSystem, Path file) {
    val factory = new CompressionCodecFactory(fileSystem.getConf());

    try {
      val codec = factory.getCodec(file);
      InputStream inputStream =
          (codec == null) ? fileSystem.open(file) : codec.createInputStream(fileSystem.open(file));
      return new DataInputStream(inputStream);
    } catch (IOException e) {
      throw new RuntimeException("Error reading: '" + file.toString() + "'", e);
    }
  }

  @SneakyThrows
  private MappingIterator<KVError> getErrors(InputStream inputStream) {
    val reader = new ObjectMapper().reader().withType(KVError.class);

    return reader.readValues(inputStream);
  }

}
