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
package org.icgc.dcc.submission.validation.first;

import static com.google.common.collect.ImmutableList.copyOf;

import java.io.DataInputStream;
import java.util.List;
import java.util.regex.Pattern;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.CompressionCodecFactory;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.fs.SubmissionDirectory;
import org.icgc.dcc.submission.validation.core.ValidationContext;

import com.google.common.collect.Lists;

/**
 * 
 */
@RequiredArgsConstructor
public class FPVFileSystem {

  private final DccFileSystem dccFileSystem;
  private final SubmissionDirectory submissionDirectory;

  public Iterable<String> listRelevantFiles(ValidationContext context) {
    // Selective validation filtering
    val fileSchemata = context.getDictionary().getFileSchemata(context.getDataTypes());

    val patterns = Lists.<String> newArrayList();
    for (val fileSchema : fileSchemata) {
      patterns.add(fileSchema.getPattern());
    }

    return context.getSubmissionDirectory().listFiles(patterns);
  }

  public List<String> getMatchingFileNames(String pattern) {
    return copyOf(submissionDirectory.listFile(Pattern.compile(pattern)));
  }

  @SneakyThrows
  public DataInputStream getDataInputStream(String filename) {
    return dccFileSystem.open(submissionDirectory.getDataFilePath(filename));
  }

  /**
   * TODO: move to {@link DccFileSystem} directly...
   */
  public CompressionCodec getCodec(String filename) {
    return new CompressionCodecFactory(dccFileSystem.getFileSystem().getConf())
        .getCodec(new Path(
            submissionDirectory.getDataFilePath(filename)));
  }

}
