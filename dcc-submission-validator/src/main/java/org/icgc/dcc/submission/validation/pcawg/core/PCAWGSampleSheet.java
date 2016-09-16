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
package org.icgc.dcc.submission.validation.pcawg.core;

import static com.google.common.base.Suppliers.memoizeWithExpiration;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.toList;
import static org.icgc.dcc.common.core.json.Jackson.DEFAULT;
import static org.icgc.dcc.common.core.util.stream.Collectors.toImmutableSet;

import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Resources;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PCAWGSampleSheet {

  /**
   * Constants.
   */
  public static final URL DEFAULT_PCAWG_SAMPLE_SHEET_URL = Resources.getResource("pcawg-sample-sheet.json");

  /**
   * Configuration.
   */
  @NonNull
  private final URL url;

  /**
   * State.
   */
  private final Supplier<List<PCAWGSample>> supplier = memoizeWithExpiration(this::readSamples, 10, MINUTES);

  public PCAWGSampleSheet() {
    this(DEFAULT_PCAWG_SAMPLE_SHEET_URL);
  }

  public boolean hasProject(@NonNull String projectKey) {
    val projectNames = getProjects();

    return projectNames.contains(projectKey);
  }

  public Set<String> getProjects() {
    return samples().stream().map(PCAWGSample::getProjectKey).collect(toImmutableSet());
  }

  public List<PCAWGSample> getProjectSamples(@NonNull String projectKey) {
    return samples().stream().filter(sample -> sample.getProjectKey().equals(projectKey)).collect(toList());
  }

  public Multimap<String, String> getProjectDonorIds() {
    return getProjectFields(PCAWGSample::getDonorId);
  }

  public Multimap<String, String> getProjectSpecimenIds() {
    return getProjectFields(PCAWGSample::getSpecimenId);
  }

  public Multimap<String, String> getProjectSampleIds() {
    return getProjectFields(PCAWGSample::getSampleId);
  }

  private Multimap<String, String> getProjectFields(@NonNull Function<PCAWGSample, String> accessor) {
    // Keep unique values only
    val builder = ImmutableSetMultimap.<String, String> builder();

    for (val sample : samples()) {
      val projectKey = sample.getProjectKey();
      val fieldValue = accessor.apply(sample);
      builder.put(projectKey, fieldValue);
    }

    return builder.build();
  }

  private List<PCAWGSample> samples() {
    return supplier.get();
  }

  @SneakyThrows
  private List<PCAWGSample> readSamples() {
    log.info("Refreshing sample sheet...");
    return DEFAULT.readValue(url, new TypeReference<List<PCAWGSample>>() {});
  }

}
