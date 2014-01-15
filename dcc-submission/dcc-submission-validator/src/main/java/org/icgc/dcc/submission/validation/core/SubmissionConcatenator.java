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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Maps.newTreeMap;
import static org.icgc.dcc.submission.core.parser.FileParsers.newStringFileParser;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.NavigableMap;
import java.util.regex.Pattern;

import lombok.Cleanup;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.icgc.dcc.submission.core.parser.FileRecordProcessor;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.SubmissionDirectory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;

@Slf4j
@RequiredArgsConstructor
public class SubmissionConcatenator {

  public static final String CONCAT_DIR_NAME = ".concatenation";
  public static final String CONCAT_FILE_EXTENSION = "txt";

  @NonNull
  private final FileSystem fileSystem;
  @NonNull
  private final Dictionary dictionary;

  // TODO: throw checked exception?
  @SneakyThrows
  public List<SubmissionConcatFile> concat(SubmissionDirectory submissionDirectory) {
    log.info("Concatenating: {}", submissionDirectory);

    val concatFiles = ImmutableList.<SubmissionConcatFile> builder();
    val concatDirectory = getConcatDirectory(submissionDirectory);
    val concatPartFiles = getSubmissionFilesByType(submissionDirectory);

    int concatFileNumber = 1;
    int concatFileCount = concatPartFiles.size();
    for (val fileType : concatPartFiles.keySet()) {
      val partFiles = concatPartFiles.get(fileType);
      val concatFilePath = getConcatFilePath(concatDirectory, fileType);

      log.info("  - [{}/{}] Concatenating file '{}' to '{}'",
          new Object[] { concatFileNumber, concatFileCount, fileType, concatFilePath });
      val concatFile = concatFileType(concatFilePath, fileType, partFiles);

      concatFiles.add(concatFile);
      concatFileNumber++;
    }

    return concatFiles.build();
  }

  private SubmissionConcatFile concatFileType(Path concatFilePath, SubmissionFileType fileType, List<Path> partFiles)
      throws Exception {
    @Cleanup
    val concatWriter = new PrintWriter(fileSystem.create(concatFilePath));
    val concatFile = new SubmissionConcatFile(fileType, concatFilePath);

    int partNumber = 1;
    int partTotalCount = partFiles.size();

    for (val partFile : partFiles) {
      val writeHeader = partNumber == 1;
      val partFileParser = newStringFileParser(fileSystem, true);

      log.info("    * [{}/{}] Concatenating part file '{}'", new Object[] { partNumber, partTotalCount, partFile });
      val lineCount = partFileParser.parse(partFile, new FileRecordProcessor<String>() {

        @Override
        public void process(long lineNumber, String record) throws Exception {
          if (!writeHeader && lineNumber == 1) {
            return;
          }

          concatWriter.println(record);
        }

      });

      concatFile.addPart(partFile, lineCount);
      partNumber++;
    }

    return concatFile;
  }

  private ListMultimap<SubmissionFileType, Path> getSubmissionFilesByType(SubmissionDirectory submissionDirectory) {
    val basePath = submissionDirectory.getSubmissionDirPath();
    val map = ImmutableListMultimap.<SubmissionFileType, Path> builder();

    for (val fileSchema : dictionary.getFiles()) {
      val fileType = SubmissionFileType.from(fileSchema.getName());
      val fileRegex = fileSchema.getPattern();
      val fileNames = submissionDirectory.listFile(Pattern.compile(fileRegex));

      for (val fileName : fileNames) {
        map.put(fileType, new Path(basePath, fileName));
      }
    }

    return map.build();
  }

  public static Path getConcatDirectory(SubmissionDirectory submissionDirectory) {
    val parentPath = submissionDirectory.getValidationDirPath();

    return new Path(parentPath, CONCAT_DIR_NAME);
  }

  private static Path getConcatFilePath(Path concatDirectory, SubmissionFileType fileType) {
    val baseName = fileType.getTypeName().toLowerCase();
    val fileName = baseName + "." + CONCAT_FILE_EXTENSION;

    return new Path(concatDirectory, fileName);
  }

  @RequiredArgsConstructor
  public static class SubmissionConcatFile {

    private static final long HEADER_LINE_COUNT = 1;

    private final NavigableMap<Long, Path> partLineMapping = newTreeMap();

    @NonNull
    @Getter
    private final SubmissionFileType fileType;

    @NonNull
    @Getter
    private final Path path;
    @Getter
    private long lineCount = HEADER_LINE_COUNT;

    public void addPart(Path partPath, long partLineCount) {
      lineCount += partLineCount - HEADER_LINE_COUNT;

      partLineMapping.put(lineCount, partPath);
    }

    public ConcatenationCoordinate getCoordinates(long lineNumber) {
      checkArgument(lineNumber != HEADER_LINE_COUNT, "Ambiguous line number for header");
      checkArgument(lineNumber > 0, "Line number must be positive");
      checkArgument(lineNumber <= lineCount, "Line number is out of range");

      val entry = partLineMapping.floorEntry(lineNumber);
      val originalFilePath = entry.getValue();
      val relativeLineNumber = lineNumber - entry.getKey() + HEADER_LINE_COUNT; // TODO: check OBOE
      return new ConcatenationCoordinate(originalFilePath, relativeLineNumber);
    }

    /**
     * Respects the order of addition.
     */
    public Collection<Path> getParts() {
      return partLineMapping.values();
    }

  }

  @Value
  public static class ConcatenationCoordinate {

    private final Path originalPath;
    private final Long originalLineNumber;
  }

}
