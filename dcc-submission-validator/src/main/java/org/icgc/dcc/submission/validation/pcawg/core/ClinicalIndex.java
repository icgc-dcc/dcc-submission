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
import static org.icgc.dcc.submission.validation.pcawg.core.ClinicalFields.getSampleSampleId;
import static org.icgc.dcc.submission.validation.pcawg.core.ClinicalFields.getSampleSpecimenId;
import static org.icgc.dcc.submission.validation.pcawg.core.ClinicalFields.getSpecimenDonorId;
import static org.icgc.dcc.submission.validation.pcawg.core.ClinicalFields.getSpecimenSpecimenId;

import java.util.List;
import java.util.Map;

import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.submission.core.model.Record;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

public class ClinicalIndex {

  /**
   * Data.
   */
  @NonNull
  private final Clinical clinical;

  /**
   * Indexes.
   */
  @NonNull
  private final Map<String, Record> donorIndex;
  @NonNull
  private final Map<String, Record> specimenIndex;
  @NonNull
  private final Map<String, Record> sampleIndex;

  @NonNull
  private final Multimap<String, String> donorSpecimenIndex;
  @NonNull
  private final Multimap<String, String> specimenSampleIndex;

  public ClinicalIndex(@NonNull Clinical clinical) {
    this.clinical = clinical;

    //
    // Index donors
    //

    {
      val donorIndex = ImmutableMap.<String, Record> builder();
      for (val donor : clinical.getCore().getDonors()) {
        val donorId = getDonorDonorId(donor);

        donorIndex.put(donorId, donor);
      }
      this.donorIndex = donorIndex.build();
    }

    //
    // Index samples
    //

    {
      val sampleIndex = ImmutableMap.<String, Record> builder();
      val specimenSampleIndex = ImmutableMultimap.<String, String> builder();
      for (val sample : clinical.getCore().getSamples()) {
        val sampleId = getSampleSampleId(sample);
        val sampleSpecimenId = getSampleSpecimenId(sample);

        sampleIndex.put(sampleId, sample);
        specimenSampleIndex.put(sampleSpecimenId, sampleId);
      }
      this.sampleIndex = sampleIndex.build();
      this.specimenSampleIndex = specimenSampleIndex.build();
    }

    //
    // Index specimen
    //

    {
      val specimenIndex = ImmutableMap.<String, Record> builder();
      val donorSpecimenIndex = ImmutableMultimap.<String, String> builder();
      for (val specimen : clinical.getCore().getSpecimens()) {
        val specimenId = getSpecimenSpecimenId(specimen);
        val specimenDonorId = getSpecimenDonorId(specimen);

        specimenIndex.put(specimenId, specimen);
        donorSpecimenIndex.put(specimenDonorId, specimenId);
      }
      this.specimenIndex = specimenIndex.build();
      this.donorSpecimenIndex = donorSpecimenIndex.build();
    }
  }

  public Record getDonor(@NonNull String donorId) {
    return donorIndex.get(donorId);
  }

  public List<Record> getDonorSamples(@NonNull String donorId) {
    val donorSamples = ImmutableList.<Record> builder();

    val donorSpecimens = getDonorSpecimen(donorId);
    for (val donorSpecimen : donorSpecimens) {
      val specimenId = getSpecimenSpecimenId(donorSpecimen);
      val specimenSamples = getSpecimenSamples(specimenId);

      donorSamples.addAll(specimenSamples);
    }

    return donorSamples.build();
  }

  public List<Record> getDonorSpecimen(@NonNull String donorId) {
    val donorSpecimens = ImmutableList.<Record> builder();
    for (val donorSpecimenId : donorSpecimenIndex.get(donorId)) {
      val donorSpecimen = getSpecimen(donorSpecimenId);

      donorSpecimens.add(donorSpecimen);
    }

    return donorSpecimens.build();
  }

  public Record getSpecimen(@NonNull String specimenId) {
    return specimenIndex.get(specimenId);
  }

  public Record getSpecimenDonor(@NonNull String specimenId) {
    val specimen = getSpecimen(specimenId);
    val donorId = getSpecimenDonorId(specimen);

    return getDonor(donorId);
  }

  public List<Record> getSpecimenSamples(@NonNull String specimenId) {
    val specimenSamples = ImmutableList.<Record> builder();
    for (val sampleId : specimenSampleIndex.get(specimenId)) {
      val specimenSample = getSample(sampleId);

      specimenSamples.add(specimenSample);
    }

    return specimenSamples.build();
  }

  public Record getSample(@NonNull String sampleId) {
    return sampleIndex.get(sampleId);
  }

  public Record getSampleDonor(@NonNull String sampleId) {
    val sample = getSample(sampleId);
    val specimenId = getSampleSpecimenId(sample);

    return getSpecimenDonor(specimenId);
  }

  public Record getSampleSpecimen(@NonNull String sampleId) {
    val sample = getSample(sampleId);
    val specimenId = getSampleSpecimenId(sample);

    return getSpecimen(specimenId);
  }

}
