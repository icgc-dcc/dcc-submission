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

import org.apache.hadoop.io.compress.CompressionCodec;
import org.icgc.dcc.submission.fs.SubmissionDirectory;

/**
 * Class representing interactions with the file system in the context of FPV (as a temporary measure to isolate such
 * operations from the FPV at first).
 */
@RequiredArgsConstructor
public class FPVFileSystem {

  private final SubmissionDirectory submissionDirectory;

  public Iterable<String> listMatchingSubmissionFiles(List<String> filePatterns) {
    return submissionDirectory.listFiles(filePatterns);
  }

  public List<String> getMatchingFileNames(String pattern) {
    return copyOf(submissionDirectory.listFile(Pattern.compile(pattern)));
  }

  @SneakyThrows
  public DataInputStream getDataInputStream(String fileName) {
    return submissionDirectory.open(fileName);
  }

  public CompressionCodec getCompressionCodec(String fileName) {
    return submissionDirectory.getCompressionCodec(fileName);
  }

}
