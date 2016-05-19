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

import org.icgc.dcc.submission.validation.core.Clinical;
import org.icgc.dcc.submission.validation.core.ClinicalCore;
import org.icgc.dcc.submission.validation.core.ClinicalIndex;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class PCAWGClinicalFilter {

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

  public void filter(@NonNull Clinical clinical) {
    val clinicalCore = clinical.getCore();
    val index = new ClinicalIndex(clinical);

    filterByDonorId(clinicalCore, index);
    filterBySpecimenId(clinicalCore, index);
    filterBySampleId(clinicalCore, index);
  }

  private void filterByDonorId(ClinicalCore clinicalCore, ClinicalIndex index) {
    val excludedDonorIds = pcawgDictionary.getExcludedDonorIds(projectKey);
    for (val excludedDonorId : excludedDonorIds) {
      log.info("Excluding donor '{}'...", excludedDonorId);

      // Identify donor records
      val donor = index.getDonor(excludedDonorId);
      val donorSpecimens = index.getDonorSpecimen(excludedDonorId);
      val donorSamples = index.getDonorSamples(excludedDonorId);

      // Remove core records
      clinicalCore.getDonors().remove(donor);
      clinicalCore.getSpecimens().removeAll(donorSpecimens);
      clinicalCore.getSamples().removeAll(donorSamples);
    }
  }

  private void filterBySpecimenId(ClinicalCore clinicalCore, ClinicalIndex index) {
    val excludedSpecimenIds = pcawgDictionary.getExcludedSpecimenIds(projectKey);
    for (val excludedSpecimenId : excludedSpecimenIds) {
      log.info("Excluding specimen '{}'...", excludedSpecimenId);

      // Identify specimen records
      val specimen = index.getSpecimen(excludedSpecimenId);
      val specimenSamples = index.getSpecimenSamples(excludedSpecimenId);

      // Remove core records
      clinicalCore.getSpecimens().remove(specimen);
      clinicalCore.getSamples().removeAll(specimenSamples);
    }
  }

  private void filterBySampleId(ClinicalCore clinicalCore, ClinicalIndex index) {
    val excludedSampleIds = pcawgDictionary.getExcludedSampleIds(projectKey);
    for (val excludedSampleId : excludedSampleIds) {
      log.info("Excluding sample '{}'...", excludedSampleId);

      // Identify sample record
      val sample = index.getSample(excludedSampleId);

      // Remove core records
      clinicalCore.getSamples().remove(sample);
    }
  }

}
