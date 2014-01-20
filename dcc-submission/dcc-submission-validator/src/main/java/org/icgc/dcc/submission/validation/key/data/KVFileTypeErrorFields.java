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
package org.icgc.dcc.submission.validation.key.data;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.validation.key.enumeration.KVErrorType;
import org.icgc.dcc.submission.validation.key.enumeration.KVFileType;
import org.icgc.dcc.submission.validation.key.enumeration.KeysType;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

/**
 * 
 */
@Slf4j
public class KVFileTypeErrorFields {

  private final Map<KVErrorType, List<String>> errorFieldNames;

  // TODO: factory instead of constructor
  public KVFileTypeErrorFields(
      @NonNull KVFileType fileType,
      List<String> pkNames,
      List<String> fkNames,
      List<String> secondaryFkNames) {
    checkState(pkNames != null || fkNames != null || secondaryFkNames != null, "TODO");
    this.errorFieldNames = getFieldNamesPerErrorType(pkNames, fkNames, secondaryFkNames);
    checkState(!errorFieldNames.isEmpty(), "TODO");
    log.info("fieldNamesPerErrorType: '{}'", errorFieldNames);
  }

  public List<String> getErrorFieldNames(KVErrorType errorType) {
    return errorFieldNames.get(errorType);
  }

  private final Map<KVErrorType, List<String>> getFieldNamesPerErrorType(
      List<String> pkNames, List<String> fkNames, List<String> secondaryFkNames) {
    val builder = new ImmutableMap.Builder<KVErrorType, List<String>>();
    for (val errorType : KVErrorType.values()) {
      val keysType = errorType.getKeysType();
      val optionalNames = getOptionalNames(keysType,
          pkNames, fkNames, secondaryFkNames);
      log.info("keysType, optionalIndices: {}, '({}, {})'", new Object[] { keysType, optionalNames });
      if (optionalNames.isPresent()) {
        builder.put(errorType, optionalNames.get());
      }
    }
    return builder.build();
  }

  private final Optional<List<String>> getOptionalNames(KeysType keysType,
      List<String> pkNames, List<String> fkNames, List<String> secondaryFkNames) {
    List<String> names = null;
    switch (keysType) {
    case PK:
      names = pkNames == null ? null : newArrayList(pkNames);
      break;
    case FK:
      names = fkNames == null ? null : newArrayList(fkNames);
      break;
    case SECONDARY_FK:
      names = secondaryFkNames == null ? null : newArrayList(secondaryFkNames);
      break;
    default:
      checkState(false, "%s", keysType);
    }
    return names == null ? Optional.<List<String>> absent() : Optional.of(names);
  }
}
