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
package org.icgc.dcc.submission.validation.key.data;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.Map;

import org.icgc.dcc.submission.validation.key.core.KVErrorType;
import org.icgc.dcc.submission.validation.key.core.KVFileType;
import org.icgc.dcc.submission.validation.key.core.KVKeyType;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Keeps track of the field names affected by a given {@link KVErrorType} for a given {@link KVFileType}.
 * <p>
 * TODO: revamp completely, ugly design
 */
@Slf4j
public class KVFileTypeErrorFields {

  private final KVFileType fileType;
  private final Map<KVErrorType, List<String>> errorFieldNames;

  public static class Builder {

    private final KVFileType fileType;
    private List<String> pkFieldNames;
    private List<String> fk1FieldNames;
    private List<String> fk2FieldNames;
    private List<String> optionalFkFieldNames;

    public Builder(KVFileType fileType) {
      this.fileType = fileType;
    }

    public Builder pkFieldNames(List<String> pkFieldNames) {
      this.pkFieldNames = pkFieldNames;
      return this;
    }

    public Builder fk1FieldNames(List<String> fk1FieldNames) {
      this.fk1FieldNames = fk1FieldNames;
      return this;
    }

    public Builder fk2FieldNames(List<String> fk2FieldNames) {
      this.fk2FieldNames = fk2FieldNames;
      return this;
    }

    public Builder optionalFkFieldNames(List<String> optionalFkFieldNames) {
      this.optionalFkFieldNames = optionalFkFieldNames;
      return this;
    }

    public KVFileTypeErrorFields build() {
      return new KVFileTypeErrorFields(fileType, pkFieldNames, fk1FieldNames, fk2FieldNames, optionalFkFieldNames);
    }

  }

  private KVFileTypeErrorFields(
      @NonNull KVFileType fileType,
      List<String> pkFieldNames,
      List<String> fk1FieldNames,
      List<String> fk2FieldNames,
      List<String> optionalFkFieldNames) {
    this.fileType = fileType;
    checkState(
        pkFieldNames != null || fk1FieldNames != null || optionalFkFieldNames != null,
        "There should be at least one set of names that isn't null");
    this.errorFieldNames = getFieldNamesPerErrorType(
        pkFieldNames, fk1FieldNames, fk2FieldNames, optionalFkFieldNames);
    checkState(!errorFieldNames.isEmpty());
    log.info("fieldNamesPerErrorType: '{}' for '{}'", errorFieldNames, this.fileType);
  }

  public List<String> getErrorFieldNames(KVErrorType errorType) {
    return errorFieldNames.get(errorType);
  }

  /**
   * Returns a mapping of error type to their corresponding field names for the current {@link KVFileType}.
   */
  private final Map<KVErrorType, List<String>> getFieldNamesPerErrorType(
      List<String> pkFieldNames,
      List<String> fk1FieldNames,
      List<String> fk2FieldNames,
      List<String> optionalFkFieldNames) {
    val builder = new ImmutableMap.Builder<KVErrorType, List<String>>();
    for (val errorType : KVErrorType.values()) {
      val keysType = errorType.getKeysType();
      val optionalNames = getOptionalNames(keysType,
          pkFieldNames, fk1FieldNames, fk2FieldNames, optionalFkFieldNames);
      log.info("keysType, optionalIndices: {}, '({}, {})'", new Object[] { keysType, optionalNames });
      if (optionalNames.isPresent()) {
        builder.put(errorType, optionalNames.get());
      }
    }
    return builder.build();
  }

  private final Optional<List<String>> getOptionalNames(
      KVKeyType keysType,
      List<String> pkFieldNames,
      List<String> fk1FieldNames,
      List<String> fk2FieldNames,
      List<String> optionalFkFieldNames) {
    List<String> names = null;
    switch (keysType) {
    case PK:
      names = pkFieldNames == null ? null : newArrayList(pkFieldNames);
      break;
    case FK1:
      names = fk1FieldNames == null ? null : newArrayList(fk1FieldNames);
      break;
    case FK2:
      names = fk2FieldNames == null ? null : newArrayList(fk2FieldNames);
      break;
    case OPTIONAL_FK:
      names = optionalFkFieldNames == null ? null : newArrayList(optionalFkFieldNames);
      break;
    default:
      checkState(false, "%s", keysType);
    }
    return names == null ? Optional.<List<String>> absent() : Optional.of(names);
  }

}
