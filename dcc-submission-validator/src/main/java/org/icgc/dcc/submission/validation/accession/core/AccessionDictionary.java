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
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.icgc.dcc.common.core.json.Jackson.DEFAULT;

import java.net.URL;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.icgc.dcc.submission.validation.accession.AccessionValidator;

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
  private final Supplier<Metadata> supplier = memoizeWithExpiration(this::read, 10, MINUTES);

  public AccessionDictionary() {
    this(DEFAULT_ACCESSION_DICTIONARY_URL);
  }

  public boolean isExcluded(String projectKey, String analysisId) {
    // Exclude by project key
    val excludedProjectKeys = metadata().getExcludedProjectKeys();
    if (excludedProjectKeys != null & excludedProjectKeys.contains(projectKey)) {
      return true;
    }

    // Exclude by project key, analysis id value
    val excludedAnalysisIds = metadata().getExcludedAnalysisIds().get(projectKey);
    if (excludedAnalysisIds != null && excludedAnalysisIds.contains(analysisId)) {
      return true;
    }

    // Exclude by project key, analysis id pattern
    val excludedAnalysisIdPatterns = metadata().getExcludedAnalysisIdPatterns().get(projectKey);
    if (excludedAnalysisIdPatterns != null) {
      for (val pattern : excludedAnalysisIdPatterns) {
        val exclude = pattern.matcher(analysisId).matches();
        if (exclude) {
          return true;
        }
      }
    }

    return false;
  }

  private Metadata metadata() {
    return supplier.get();
  }

  @SneakyThrows
  private Metadata read() {
    log.info("Refreshing metadata...");
    return DEFAULT.readValue(url, Metadata.class);
  }

  @Value
  private static class Metadata {

    Set<String> excludedProjectKeys = newHashSet();
    Map<String, Set<String>> excludedAnalysisIds = newHashMap();
    Map<String, Set<Pattern>> excludedAnalysisIdPatterns = newHashMap();

  }

}
