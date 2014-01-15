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
package org.icgc.dcc.submission.validation.key.core;

import static com.google.common.base.Preconditions.checkState;
import static java.util.regex.Pattern.compile;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.checkExistence;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.lsFile;
import static org.icgc.dcc.submission.validation.key.enumeration.KVFileType.DONOR;

import java.io.IOException;
import java.io.InputStream;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.validation.key.enumeration.KVExperimentalDataType;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;

@RequiredArgsConstructor
public final class KVFileSystem {

  @Getter
  @NonNull
  private final FileSystem fileSystem;
  @NonNull
  private final Dictionary dictionary;
  @NonNull
  private final Path releaseDir;

  public InputStream open(Path path) throws IOException {
    return fileSystem.open(path);
  }

  public Path getDataFilePath(KVFileType fileType) {
    val basePath = releaseDir;
    val fileSchema = getFileSchema(fileType);
    val fileRegex = fileSchema.getPattern();
    val filePattern = compile(fileRegex);
    val filePaths = lsFile(fileSystem, basePath, filePattern);
    if (filePaths.isEmpty()) {
      return null;
    }

    checkState(filePaths.size() == 1, "Expected at most 1 file path but found %s. File paths: %s",
        filePaths.size(), filePaths);

    return filePaths.get(0);
  }

  public boolean hasClinicalData() {
    return hasFile(getDataFilePath(DONOR));
  }

  public boolean hasType(KVExperimentalDataType dataType) {
    return hasFile(getDataFilePath(dataType.getTaleTellerFileType()));
  }

  private boolean hasFile(Path filePath) {
    if (filePath == null) {
      return false;
    }

    return checkExistence(fileSystem, filePath);
  }

  private FileSchema getFileSchema(KVFileType fileType) {
    val targetName = fileType.toString().toLowerCase();
    for (val fileSchema : dictionary.getFiles()) {
      if (targetName.equals(fileSchema.getName())) {
        return fileSchema;
      }
    }

    throw new IllegalArgumentException("No file schema found for file type: " + fileType);
  }

}
