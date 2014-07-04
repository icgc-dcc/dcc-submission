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
package org.icgc.dcc.submission.validation.norm;

import java.io.IOException;
import java.io.PrintWriter;

import lombok.Cleanup;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.FeatureTypes.FeatureType;
import org.icgc.dcc.hadoop.parser.FileRecordProcessor;
import org.icgc.dcc.hadoop.parser.TsvPartFileProcessor;

/**
 * "Pseudo" normalizer, simply adds a PK to {@link FeatureType#SGV_TYPE} files.
 */
@RequiredArgsConstructor
public class PseudoNormalizer {

  private final FileSystem fileSystem;
  private final String inputFilePath;
  private final String outputFilePath;

  public void process() {

    @Cleanup
    val concatWriter = getWriter();

    TsvPartFileProcessor.parseFile(
        fileSystem,
        new Path(inputFilePath),
        new FileRecordProcessor<String>() {

          @Override
          public void process(long lineNumber, String record) throws IOException {
            concatWriter.println(record);
          }

        });
  }

  @SneakyThrows
  private PrintWriter getWriter() {
    return new PrintWriter(fileSystem.create(new Path(outputFilePath)));
  }

}
