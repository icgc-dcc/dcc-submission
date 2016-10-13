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
package org.icgc.dcc.submission.validation.accession.core;

import static com.google.common.base.Suppliers.memoizeWithExpiration;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.icgc.dcc.common.core.json.Jackson.DEFAULT;

import java.net.URL;
import java.util.List;

import org.icgc.dcc.common.core.model.FileTypes.FileType;
import org.icgc.dcc.submission.validation.accession.AccessionValidator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Supplier;
import com.google.common.io.Resources;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * Externalizable metadata for accession validation.
 * <p>
 * Used for identifying "grandfathered" {@code analysis_id}s that should be excluded from validation. See
 * {@linkplain AccessionValidator} for details.
 */
@Slf4j
@RequiredArgsConstructor
public class AccessionDictionary {

  /**
   * The default location of the dictionary.
   */
  public static final URL DEFAULT_ACCESSION_DICTIONARY_URL = Resources.getResource("accession-dictionary.json");

  /**
   * The URL of the externalized JSON file.
   */
  @NonNull
  private final URL url;

  /**
   * State.
   */
  private final Supplier<List<Record>> supplier = memoizeWithExpiration(this::read, 10, MINUTES);

  public AccessionDictionary() {
    this(DEFAULT_ACCESSION_DICTIONARY_URL);
  }

  public boolean isExcluded(String projectKey, FileType fileType, String analysisId, String analyzedSampleId) {
    for (val record : records()) {
      if (record.getProjectKey().equals(projectKey) &&
          record.getFileType().equals(fileType) &&
          record.getAnalysisId().equals(analysisId) &&
          record.getAnalyzedSampleId().equals(analyzedSampleId)) {
        return true;
      }
    }

    return false;
  }

  private List<Record> records() {
    return supplier.get();
  }

  @SneakyThrows
  private List<Record> read() {
    log.info("Refreshing dictionary...");
    return DEFAULT.readValue(url, new TypeReference<List<Record>>() {});
  }

  @Value
  private static class Record {

    String projectKey;
    FileType fileType;
    String analyzedSampleId;
    String analysisId;

  }

}
