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

import static java.util.stream.Collectors.toList;

import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PCAWGSampleFilter {

  /**
   * Configuration.
   */
  @NonNull
  private final String projectKey;

  /**
   * Dependencies.
   */
  @NonNull
  private final PCAWGDictionary pcawgDictionary;

  public List<PCAWGSample> filter(@NonNull List<PCAWGSample> pcawgSamples) {
    // Resolve entities to exclude from validation
    val excludedDonorIds = pcawgDictionary.getExcludedDonorIds(projectKey);
    val excludedSpecimenIds = pcawgDictionary.getExcludedSpecimenIds(projectKey);
    val excludedSampleIds = pcawgDictionary.getExcludedSampleIds(projectKey);

    // Apply PCAWG dictionary excludes to sample sheet
    log.info("Filtering PCAWG samples...");
    return pcawgSamples.stream()
        .filter(sample -> !excludedDonorIds.contains(sample.getDonorId()))
        .filter(sample -> !excludedSpecimenIds.contains(sample.getSpecimenId()))
        .filter(sample -> !excludedSampleIds.contains(sample.getSampleId()))
        .collect(toList());
  }

}