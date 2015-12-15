/*
 * Copyright (c) 2015 The Ontario Institute for Cancer Research. All rights reserved.                             
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

import static org.icgc.dcc.common.json.Jackson.DEFAULT;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;

@RequiredArgsConstructor
public class PCAWGSampleSheet {

  /**
   * Constants.
   */
  public static final URL DEFAULT_PCAWG_SAMPLE_SHEET_URL = Resources.getResource("pcawg-sample-sheet.json");

  @NonNull
  private final URL url;

  public PCAWGSampleSheet() {
    this(DEFAULT_PCAWG_SAMPLE_SHEET_URL);
  }

  public Set<String> getProjects() {
    val projects = Sets.<String> newTreeSet();
    for (val row : readSheet()) {
      val project = row.get("projectKey");

      projects.add(project);
    }

    return ImmutableSet.copyOf(projects);
  }

  public Multimap<String, String> getProjectSampleIds() {
    // Keep unique values only
    val builder = ImmutableSetMultimap.<String, String> builder();

    for (val row : readSheet()) {
      val projectKey = row.get("projectKey");
      val sampleId = row.get("sampleId");
      builder.put(projectKey, sampleId);
    }

    return builder.build();
  }

  @SneakyThrows
  private List<Map<String, String>> readSheet() {
    return DEFAULT.readValue(url, new TypeReference<List<Map<String, String>>>() {});
  }

}
