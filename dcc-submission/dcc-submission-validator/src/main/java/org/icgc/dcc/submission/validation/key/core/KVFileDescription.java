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
package org.icgc.dcc.submission.validation.key.core;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static lombok.AccessLevel.PRIVATE;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;
import org.icgc.dcc.submission.validation.key.utils.KVConstants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Optional;

/**
 * TODO: is this still useful?
 */
@Value
@RequiredArgsConstructor(access = PRIVATE)
public class KVFileDescription {

  @NonNull
  private final KVFileType fileType;

  /**
   * May just be a placeholder (hence the optional).
   */
  private final Optional<Path> dataFilePath;

  public static KVFileDescription getFileDescription(KVFileType fileType, Path dataFilePath) {
    return new KVFileDescription(fileType, Optional.<Path> of(dataFilePath));
  }

  public static KVFileDescription getPlaceholderFileDescription(KVFileType fileType) {
    return new KVFileDescription(fileType, Optional.<Path> absent());
  }

  @JsonIgnore
  public boolean isPlaceholder() {
    return !dataFilePath.isPresent();
  }

  @JsonIgnore
  public String getDataFileName() {
    checkState(!isPlaceholder(), "TODO");
    return dataFilePath.get().getName();
  }

  @Override
  public String toString() {
    return toJsonSummaryString();
  }

  @SneakyThrows
  public String toJsonSummaryString() {
    return format("%s (%s)",
        KVConstants.MAPPER.writeValueAsString(this),
        isPlaceholder() ? "" : dataFilePath.get()); // Optional doesn't seem to print the actual value, only whether
                                                    // present or not (TODO)
  }
}
