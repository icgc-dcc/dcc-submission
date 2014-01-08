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
package org.icgc.dcc.submission.validation.core;

import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Pattern;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.icgc.dcc.submission.core.parser.FileParsers;
import org.icgc.dcc.submission.core.parser.FileRecordProcessor;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.SubmissionDirectory;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

@Slf4j
@RequiredArgsConstructor
public class SubmissionConcatenator {

  public static final String CONCAT_DIR_NAME = ".concatenation";

  @NonNull
  private final FileSystem fileSystem;
  @NonNull
  private final Dictionary dictionary;

  public void concat(SubmissionDirectory submissionDirectory) throws Exception {
    log.info("Concatenating: {}", submissionDirectory);

    val concatDirectory = getConcatDirectory(submissionDirectory);
    val map = getSubmissionFilesByType(submissionDirectory);

    for (val fileType : map.keySet()) {
      val files = map.get(fileType);

      concatFileType(concatDirectory, fileType, files);
    }
  }

  private ListMultimap<SubmissionFileType, Path> getSubmissionFilesByType(SubmissionDirectory submissionDirectory) {
    val basePath = submissionDirectory.getSubmissionDirPath();
    val map = ImmutableListMultimap.<SubmissionFileType, Path> builder();

    for (val fileSchema : dictionary.getFiles()) {
      val fileType = SubmissionFileType.from(fileSchema.getName());
      val regex = fileSchema.getPattern();

      val fileNames = submissionDirectory.listFile(Pattern.compile(regex));
      for (val fileName : fileNames) {
        map.put(fileType, new Path(basePath, fileName));
      }
    }

    return map.build();
  }

  private void concatFileType(Path concatDirectory, SubmissionFileType fileType, List<Path> files) throws Exception {
    val concatFile = getConcatFile(concatDirectory, fileType);
    log.info("  - Concatenating file '{}' to '{}'", fileType, concatFile);

    @Cleanup
    val concatWriter = new PrintWriter(fileSystem.create(concatFile));

    for (val file : files) {
      val fileParser = FileParsers.newStringFileParser();

      log.info("    * Concatenating file entry '{}'", file);
      fileParser.parse(file, new FileRecordProcessor<String>() {

        @Override
        public void process(long lineNumber, String record) throws Exception {
          concatWriter.println(record);
        }

      });
    }
  }

  private static Path getConcatDirectory(SubmissionDirectory submissionDirectory) {
    val parentPath = submissionDirectory.getValidationDirPath();

    return new Path(parentPath, CONCAT_DIR_NAME);
  }

  private static Path getConcatFile(Path concatDirectory, SubmissionFileType fileType) {
    val baseName = fileType.getTypeName().toLowerCase();
    val fileName = baseName + ".txt";

    return new Path(concatDirectory, fileName);
  }

}
