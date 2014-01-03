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
package org.icgc.dcc.submission.validation.key;

import static java.lang.String.format;
import static org.icgc.dcc.submission.validation.key.KVConstants.MAPPER;
import static org.icgc.dcc.submission.validation.key.enumeration.KVSubmissionType.EXISTING_FILE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVSubmissionType.INCREMENTAL_FILE;
import static org.icgc.dcc.submission.validation.key.enumeration.KVSubmissionType.INCREMENTAL_TO_BE_TREATED_AS_EXISTING;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;

import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;
import org.icgc.dcc.submission.validation.key.enumeration.KVSubmissionType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Optional;

/**
 * TODO: rename
 */
@Value
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class KVFileDescription {

  @NonNull
  private final KVSubmissionType submissionType;
  @NonNull
  private final KVFileType fileType;

  /**
   * May just be a placeholder (hence the optional).
   */
  private final Optional<String> dataFilePath;

  public static KVFileDescription getExistingFileDescription(KVFileType fileType, String dataFilePath) {
    return new KVFileDescription(
        EXISTING_FILE,
        fileType,
        Optional.<String> of(dataFilePath));
  }

  public static KVFileDescription getIncrementalFileDescription(
      boolean asOriginal, KVFileType fileType, String dataFilePath) {
    return new KVFileDescription(
        asOriginal ? INCREMENTAL_TO_BE_TREATED_AS_EXISTING : INCREMENTAL_FILE,
        fileType,
        Optional.<String> of(dataFilePath));
  }

  public static KVFileDescription getPlaceholderFileDescription(KVFileType fileType) {
    return new KVFileDescription(
        EXISTING_FILE, // TODO: correct? does it matter?
        fileType,
        Optional.<String> absent());
  }

  @JsonIgnore
  public boolean isPlaceholder() {
    return !dataFilePath.isPresent();
  }

  @Override
  public String toString() {
    return toJsonSummaryString();
  }

  @SneakyThrows
  public String toJsonSummaryString() {
    return format("%s (%s)",
        MAPPER.writeValueAsString(this),
        isPlaceholder() ? "" : dataFilePath.get()); // Optional doesn't seem to print the actual value, only whether
                                                    // present or not (TODO)
  }
}
