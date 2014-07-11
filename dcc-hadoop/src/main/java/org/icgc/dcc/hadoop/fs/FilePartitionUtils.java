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

import static org.icgc.dcc.hadoop.parser.FileParsers.newStringFileParser;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.hadoop.parser.FileRecordProcessor;

import com.google.common.collect.Lists;
import com.google.common.io.Files;

@Slf4j
public class FilePartitionUtils {

  private final static String HDFS_PART_FILE_PREFIX = "part";

  // Merge files, this assumes that the header exists on all input files
  public static void merge(FileSystem fs, List<Path> input, Path output) throws IOException {
    @Cleanup
    val writer = getWriter(fs, output);
    val parser = newStringFileParser(fs, true);
    val processor = new FileRecordProcessor<String>() {

      boolean firstFile = true;

      @Override
      public void process(long lineNumber, String record) throws IOException {
        if (lineNumber == 1) {
          if (firstFile == true) {
            writer.println(record);
            firstFile = false;
          }
        } else {
          writer.println(record);
        }
      }
    };

    for (Path path : input) {
      parser.parse(path, processor);
    }
  }

  // Repartition part files {C1, C2, ... Cn} to {D1, D2, D3, ... Dk}
  // Assumes the first file has a header
  public static void repartition(final FileSystem fs, final Path inputPath, final Path outputPath,
      final String outFile, final long sizeBytes)
      throws IOException {
    log.info("Repartitioning {} into {} byte chunks", inputPath, sizeBytes);

    boolean isDirectory = true;
    List<Path> inputFiles = Lists.newArrayList();

    if (fs.isDirectory(inputPath)) {
      inputFiles.addAll(HadoopUtils.lsAll(fs, inputPath));
    } else if (fs.isFile(inputPath)) {
      inputFiles.add(inputPath);
      isDirectory = false;
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
          writerHandle.get().flush();
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

    for (Path file : inputFiles) {
      if (isDirectory && !file.getName().startsWith(HDFS_PART_FILE_PREFIX)) continue;
      parser.parse(file, processor);
    }
    writerHandle.get().flush();
    writerHandle.get().close();
  }

  private static Path repartitionPath(Path outPath, String fileName, int partNum) {
    val extension = Files.getFileExtension(fileName);
    val name = Files.getNameWithoutExtension(fileName);
    val newName = name + "." + partNum + "." + extension;

    return new Path(outPath.toUri().getPath() + "/" + newName);
  }

  @SneakyThrows
  private static PrintWriter getWriter(FileSystem fs, Path p) {
    return new PrintWriter(fs.create(p));
  }

}
