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

import static org.icgc.dcc.submission.validation.pcawg.core.ClinicalFields.getDonorDonorId;
import static org.icgc.dcc.submission.validation.pcawg.core.ClinicalFields.getDonorId;
import static org.icgc.dcc.submission.validation.pcawg.core.ClinicalFields.getSpecimenSpecimenId;
import static org.icgc.dcc.submission.validation.pcawg.core.PCAWGFields.isPCAWGSample;
import static org.icgc.dcc.submission.validation.util.Streams.filter;

import java.util.List;
import java.util.Set;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.model.Record;

import com.google.common.collect.Sets;

@Slf4j
@RequiredArgsConstructor
public class PCAWGClinical {

  // TODO:
  // 1) Change this class to PCAWGFilter + filter() method
  // 2) Use Clinical as PCAWGClinical's replacement
  // 3) Decompose PCAWGValidator into 2 sub validators for Core and Clinical
  // 4) Move remaining control logic into PCAWGValidator
  // 5) Consider externalizing all rules by making generic RuleValidator

  /**
   * Data.
   */
  @NonNull
  private final Clinical clinical;
  @NonNull
  private final ClinicalIndex index;

  public ClinicalOptional getOptional(Set<String> donorIds) {
    log.info("Resolving PCAWG clinical 'optional' data...");
    val family = getOptional(clinical.getOptional().getFamily(), donorIds);
    val exposure = getOptional(clinical.getOptional().getExposure(), donorIds);
    val therapy = getOptional(clinical.getOptional().getTherapy(), donorIds);
    log.info("Finished resolving PCAWG clinical 'optional' data");

    return new ClinicalOptional(null, family, exposure, null, therapy);
  }

  public ClinicalCore getCore() {
    log.info("Resolving PCAWG clinical core data...");
    val samples = getSamples();
    val specimens = getSpecimens();
    val donors = getDonors();
    log.info("Finished resolving PCAWG clinical core data...");

    return new ClinicalCore(donors, specimens, samples);
  }

  public List<Record> getSamples() {
    return filter(clinical.getCore().getSamples(), sample -> isPCAWGSample(sample));
  }

  public List<Record> getSpecimens() {
    return filter(clinical.getCore().getSpecimens(), specimen -> {
      for (Record sample : index.getSpecimenSamples(getSpecimenSpecimenId(specimen))) {
        if (isPCAWGSample(sample)) {
          return true;
        }
      }

      return false;
    });
  }

  public List<Record> getDonors() {
    return filter(clinical.getCore().getDonors(), donor -> {
      for (Record sample : index.getDonorSamples(getDonorDonorId(donor))) {
        if (isPCAWGSample(sample)) {
          return true;
        }
      }

      return false;
    });
  }

  public Set<String> getDonorIds() {
    val donorIds = Sets.<String> newTreeSet();
    for (val donor : getDonors()) {
      val donorId = getDonorDonorId(donor);

      donorIds.add(donorId);
    }

    return donorIds;
  }

  public List<Record> getOptional(List<Record> records, Set<String> donorIds) {
    return filter(records, record -> donorIds.contains(getDonorId(record)));
  }

}