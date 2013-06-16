/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.sftp.fs;

import static com.google.common.base.Throwables.propagateIfInstanceOf;
import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;

import java.io.IOException;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = PRIVATE)
public final class HdfsFileUtils {

  /**
   * Apache MINA exception handling method designed to evade Java's checked exception mechanism to propagate
   * {@code IOException}s to avoid terminating MINA SFTP sessions.
   * 
   * @param type - the return type
   * @param message - the exception message
   * @param args - the exception message arguments
   * @return nothing
   */
  public static <T> T handleException(String message, String... args) {
    return handleException(new IOException(format(message, (Object[]) args)));
  }

  /**
   * Apache MINA exception handling method designed to evade Java's checked exception mechanism to propagate
   * {@code IOException}s to avoid terminating MINA SFTP sessions.
   * 
   * @param type - the return type
   * @param e - the exception to propagate
   * @return nothing
   */
  @SneakyThrows
  public static <T> T handleException(Exception e) {
    log.warn("SFTP user triggered exception: {}", e.getMessage());
    propagateIfInstanceOf(e, IOException.class);
    throw new IOException(e);
  }

}
