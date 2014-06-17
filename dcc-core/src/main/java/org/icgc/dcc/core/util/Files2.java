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
package org.icgc.dcc.core.util;

import static com.google.common.base.Charsets.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;

import com.google.common.io.Files;

/**
 * FIXME!!
 */
public class Files2 {

  @SneakyThrows
  public static String getFirstLine(final String path) {
    if (path.endsWith(".gz") || path.endsWith(".bz2")) {
      @Cleanup
      val bufferedReader = getBufferedReader(path);
      return bufferedReader.readLine();
    } else {
      return Files.readFirstLine(
          new File(path),
          UTF_8);
    }
  }

  @SneakyThrows
  private static BufferedReader getBufferedReader(final String path) {
    FileInputStream fileInputStream = new FileInputStream(path);
    GZIPInputStream gZIPInputStream = new GZIPInputStream(fileInputStream);
    InputStreamReader inputStreamReader = new InputStreamReader(gZIPInputStream);
    val bufferedReader = new BufferedReader(inputStreamReader);
    return bufferedReader;
  }

}
