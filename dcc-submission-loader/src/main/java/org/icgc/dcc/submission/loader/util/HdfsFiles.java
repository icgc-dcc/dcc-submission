/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.loader.util;

import static lombok.AccessLevel.PRIVATE;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.loader.core.DependencyFactory;

import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

@NoArgsConstructor(access = PRIVATE)
public final class HdfsFiles {

  @SneakyThrows
  public static BufferedReader getCompressionAgnosticBufferedReader(@NonNull Path file) {
    return isCompressed(file) ? getCompressedBufferedReader(file) : getBufferedReader(file);
  }

  @SneakyThrows
  private static BufferedReader getCompressedBufferedReader(@NonNull Path file) {
    val codecFactory = DependencyFactory.getInstance().getCompressionCodecFactory();
    val fs = DependencyFactory.getInstance().getFileSystem();
    val codec = codecFactory.getCodec(file);

    val inputStreamReader = new InputStreamReader(codec.createInputStream(fs.open(file)));

    return new BufferedReader(inputStreamReader);
  }

  @SneakyThrows
  private static BufferedReader getBufferedReader(@NonNull Path file) {
    val fs = DependencyFactory.getInstance().getFileSystem();
    val inputStreamReader = new InputStreamReader(fs.open(file));

    return new BufferedReader(inputStreamReader);
  }

  static boolean isCompressed(Path file) {
    val fileName = file.getName();

    return fileName.endsWith(".gz") || fileName.endsWith(".bz2");
  }

}
