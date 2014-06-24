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
package org.icgc.dcc.hadoop.parser;

import static lombok.AccessLevel.PRIVATE;

import java.util.List;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;

@NoArgsConstructor(access = PRIVATE)
public class FileParsers {

  public static final FileSystem DEFAULT_FILE_SYSTEM = getDefaultFileSystem();

  public static FileParser<String[]> newArrayFileParser() {
    return newArrayFileParser(DEFAULT_FILE_SYSTEM);
  }

  public static FileParser<String[]> newArrayFileParser(FileSystem fileSystem) {
    return newArrayFileParser(fileSystem, false);
  }

  public static FileParser<String[]> newArrayFileParser(FileSystem fileSystem, boolean processHeader) {
    return new FileParser<String[]>(fileSystem, new FileLineArrayParser(), processHeader);
  }

  public static FileParser<Iterable<String>> newIterableFileParser() {
    return newIterableFileParser(DEFAULT_FILE_SYSTEM);
  }

  public static FileParser<Iterable<String>> newIterableFileParser(FileSystem fileSystem) {
    return newIterableFileParser(fileSystem, false);
  }

  public static FileParser<Iterable<String>> newIterableFileParser(FileSystem fileSystem, boolean processHeader) {
    return new FileParser<Iterable<String>>(fileSystem, new FileLineIterableParser(), processHeader);
  }

  public static FileParser<List<String>> newListFileParser() {
    return newListFileParser(DEFAULT_FILE_SYSTEM);
  }

  public static FileParser<List<String>> newListFileParser(FileSystem fileSystem) {
    return newListFileParser(fileSystem, false);
  }

  public static FileParser<List<String>> newListFileParser(FileSystem fileSystem, boolean processHeader) {
    return new FileParser<List<String>>(fileSystem, new FileLineListParser(), processHeader);
  }

  public static FileParser<String> newStringFileParser() {
    return newStringFileParser(DEFAULT_FILE_SYSTEM);
  }

  public static FileParser<String> newStringFileParser(FileSystem fileSystem) {
    return newStringFileParser(fileSystem, false);
  }

  public static FileParser<String> newStringFileParser(FileSystem fileSystem, boolean processHeader) {
    return new FileParser<String>(fileSystem, new FileLineStringParser(), processHeader);
  }

  @SneakyThrows
  private static FileSystem getDefaultFileSystem() {
    return FileSystem.getLocal(new Configuration());
  }

}
