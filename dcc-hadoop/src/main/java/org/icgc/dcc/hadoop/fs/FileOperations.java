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
package org.icgc.dcc.hadoop.fs;

import static com.google.common.io.Files.getFileExtension;
import static com.google.common.io.Files.getNameWithoutExtension;
import static lombok.AccessLevel.PRIVATE;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.isPartFile;
import static org.icgc.dcc.hadoop.parser.FileParsers.newStringFileParser;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Cleanup;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.hadoop.parser.FileRecordProcessor;

import com.google.common.collect.Lists;

/**
 * File level utilities.
 * <p>
 * TODO: merge with {@link HadoopUtils}?
 */
@Slf4j
@NoArgsConstructor(access = PRIVATE)
public class FileOperations {

  /**
   * Merge files, assumes that a header line exists on all input files
   */
  public static void merge(FileSystem fs, List<Path> input, Path output) throws IOException {
    @Cleanup
    val writer = getWriter(fs, output);
    val parser = newStringFileParser(fs, true);
    val processor = new FileRecordProcessor<String>() {

      boolean firstFile = true;

      @Override
      public void process(long lineNumber, String record) throws IOException {
        if (lineNumber == 1) {
          if (firstFile) {
            writer.println(record);
            firstFile = false;
          }
        } else {
          writer.println(record);
        }
      }
    };

    for (val path : input) {
      parser.parse(path, processor);
    }
  }

  /**
   * Repartition part files {C1, C2, ... Cn} into {D1, D2, D3, ... Dk}. Each file has an approximate upper bound size of
   * <code>sizeBytes</code>, note size calculation is by line and not per character.
   * 
   * Assumes the header line is in the first part file.
   */
  public static void repartition(final FileSystem fs, final Path inputPath, final Path outputPath,
      final String outFile, final long sizeBytes)
      throws IOException {
    log.info("Repartitioning {} into {} byte chunks", inputPath, sizeBytes);

    List<Path> inputFiles = Lists.newArrayList();
    FileStatus fStatus = fs.getFileStatus(inputPath);
    if (fStatus.isDirectory()) {
      inputFiles.addAll(HadoopUtils.lsAll(fs, inputPath));
    } else {
      inputFiles.add(inputPath);
    }

    val parser = newStringFileParser(fs, true);
    val writerHandle = new AtomicReference<PrintWriter>();
    val processor = new FileRecordProcessor<String>() {

      long currentBytes = 0;
      int currentPart = 0;
      String header = null;
      boolean firstFile = true;

      @Override
      public void process(long lineNumber, String record) throws IOException {
        // Handle header
        if (firstFile && lineNumber == 1) {
          header = record;
          firstFile = false;
          writerHandle.set(getWriter(fs, repartitionPath(outputPath, outFile, currentPart)));
          writerHandle.get().println(header);
          return;
        }

        if (currentBytes >= sizeBytes) {
          writerHandle.get().close();
          ++currentPart;
          currentBytes = 0;
          writerHandle.set(getWriter(fs, repartitionPath(outputPath, outFile, currentPart)));
          writerHandle.get().println(header);
        }

        writerHandle.get().println(record);
        currentBytes += record.getBytes().length;
      }
    };

    for (val file : inputFiles) {
      if (fStatus.isDirectory() && !isPartFile(file)) continue;
      parser.parse(file, processor);
    }
    writerHandle.get().close();
  }

  private static Path repartitionPath(Path outPath, String fileName, int partNum) {
    val extension = getFileExtension(fileName);
    val name = getNameWithoutExtension(fileName);
    val newName = name + "." + partNum + "." + extension;

    return new Path(outPath.toUri().getPath(), newName);
  }

  @SneakyThrows
  private static PrintWriter getWriter(FileSystem fs, Path path) {
    return new PrintWriter(fs.create(path));
  }

}
