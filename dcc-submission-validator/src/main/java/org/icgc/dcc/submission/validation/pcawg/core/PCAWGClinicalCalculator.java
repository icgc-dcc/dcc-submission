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

import static org.icgc.dcc.submission.validation.pcawg.util.ClinicalFields.getDonorDonorId;
import static org.icgc.dcc.submission.validation.pcawg.util.ClinicalFields.getDonorId;
import static org.icgc.dcc.submission.validation.pcawg.util.ClinicalFields.getSpecimenSpecimenId;
import static org.icgc.dcc.submission.validation.pcawg.util.PCAWGFields.isPCAWGSample;
import static org.icgc.dcc.submission.validation.util.Streams.filter;

import java.util.List;
import java.util.Set;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.model.Record;
import org.icgc.dcc.submission.validation.pcawg.util.ClinicalIndex;

import com.google.common.collect.Sets;

@Slf4j
@RequiredArgsConstructor
public class PCAWGClinicalCalculator {

  /**
   * Data.
   */
  @NonNull
  private final Clinical clinical;
  @NonNull
  private final ClinicalIndex index;

  public Clinical calculate() {
    val core = getCore();
    val donorIds = getDonorIds();
    val optional = getOptional(donorIds);

    return new Clinical(core, optional);
  }

  private ClinicalCore getCore() {
    log.info("Resolving PCAWG clinical core data...");
    val samples = getSamples();
    val specimens = getSpecimens();
    val donors = getDonors();
    log.info("Finished resolving PCAWG clinical core data...");
  
    return new ClinicalCore(donors, specimens, samples);
  }

  private ClinicalOptional getOptional(Set<String> donorIds) {
    val optional = clinical.getOptional();

    log.info("Resolving PCAWG clinical 'optional' data...");
    val biomarker = getOptional(optional.getBiomarker(), donorIds);
    val family = getOptional(optional.getFamily(), donorIds);
    val exposure = getOptional(optional.getExposure(), donorIds);
    val surgery = getOptional(optional.getSurgery(), donorIds);
    val therapy = getOptional(optional.getTherapy(), donorIds);
    log.info("Finished resolving PCAWG clinical 'optional' data");

    return new ClinicalOptional(biomarker, family, exposure, surgery, therapy);
  }

  private List<Record> getSamples() {
    return filter(clinical.getCore().getSamples(), sample -> isPCAWGSample(sample));
  }

  private List<Record> getSpecimens() {
    return filter(clinical.getCore().getSpecimens(), specimen -> {
      for (Record sample : index.getSpecimenSamples(getSpecimenSpecimenId(specimen))) {
        if (isPCAWGSample(sample)) {
          return true;
        }
      }

      return false;
    });
  }

  private List<Record> getDonors() {
    return filter(clinical.getCore().getDonors(), donor -> {
      for (Record sample : index.getDonorSamples(getDonorDonorId(donor))) {
        if (isPCAWGSample(sample)) {
          return true;
        }
      }

      return false;
    });
  }

  private Set<String> getDonorIds() {
    val donorIds = Sets.<String> newTreeSet();
    for (val donor : getDonors()) {
      val donorId = getDonorDonorId(donor);

      donorIds.add(donorId);
    }

    return donorIds;
  }

  private List<Record> getOptional(List<Record> records, Set<String> donorIds) {
    return filter(records, record -> donorIds.contains(getDonorId(record)));
  }

}