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
package org.icgc.dcc.test.mongodb;

import java.io.File;

import lombok.AllArgsConstructor;

import org.jongo.Jongo;

import com.google.common.base.Joiner;

@AllArgsConstructor
public abstract class BaseMongoImportExport {

  private static final String EXTENSION_SEPARATOR = ".";

  private static final String FILE_EXTENSION = "json";

  // TODO: @NotNull
  // @NotNull
  protected final File directory;

  // @NotNull
  protected final Jongo jongo;

  protected abstract void execute();

  protected static String getCollectionName(File collectionFile) {
    return collectionFile.getName().replaceAll("\\" + EXTENSION_SEPARATOR + FILE_EXTENSION, "");
  }

  protected static String getFileName(String collectionName) {
    return Joiner.on(EXTENSION_SEPARATOR).join(collectionName, FILE_EXTENSION);
  }
}
