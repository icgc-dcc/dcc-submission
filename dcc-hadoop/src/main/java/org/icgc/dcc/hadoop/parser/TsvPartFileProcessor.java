package org.icgc.dcc.hadoop.parser;
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

import static org.icgc.dcc.hadoop.parser.FileParsers.newStringFileParser;

import java.util.List;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

@Slf4j
public class TsvPartFileProcessor {

  public static void parseFiles(
      FileSystem fileSystem,
      List<Path> inputFiles,
      FileRecordProcessor<String> recordProcessor) {
    int partNumber = 1;
    int partTotalCount = inputFiles.size();

    for (val partFile : inputFiles) {
      val partFileParser = newStringFileParser(fileSystem, true);

      log.info("    * [{}/{}] Parsing part file '{}'", new Object[] { partNumber, partTotalCount, partFile });
      val lineCount = parseRecord(partFileParser, partFile, recordProcessor);
      log.info("    * [{}/{}] Number of lines read: '{}'", new Object[] { partNumber, partTotalCount, lineCount });

      partNumber++;
    }
  }

  @SneakyThrows
  private static long parseRecord(
      FileParser<String> partFileParser,
      Path partFile,
      FileRecordProcessor<String> recordProcessor) {
    return partFileParser.parse(partFile, recordProcessor);
  }

}
