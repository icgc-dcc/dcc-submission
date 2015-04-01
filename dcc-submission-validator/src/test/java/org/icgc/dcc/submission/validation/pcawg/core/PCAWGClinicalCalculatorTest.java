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

import static com.google.common.collect.ImmutableList.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.DONOR_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SAMPLE_TYPE;
import static org.icgc.dcc.common.core.model.FileTypes.FileType.SPECIMEN_TYPE;
import lombok.val;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.submission.core.model.Record;
import org.icgc.dcc.submission.validation.pcawg.util.ClinicalIndex;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class PCAWGClinicalCalculatorTest {

  @Test
  public void testCalculate() {
    val donor = new Record(
        ImmutableMap.of("donor_id", "d1"),
        DONOR_TYPE, new Path("donor.txt"), 1);

    val specimen = new Record(
        ImmutableMap.of(
            "donor_id", "d1",
            "specimen_id", "sp1"),
        SPECIMEN_TYPE, new Path("specimen.txt"), 1);

    val sample = new Record(
        ImmutableMap.of(
            "specimen_id", "sp1",
            "analyzed_sample_id", "sa1",
            "study", "1"),
        SAMPLE_TYPE, new Path("sample.txt"), 1);

    val clinical = createClinical(donor, specimen, sample);
    val pcawg = calculatePCAWG(clinical);

    assertThat(pcawg.getCore().getDonors()).contains(donor);
    assertThat(pcawg.getCore().getSpecimens()).contains(specimen);
    assertThat(pcawg.getCore().getSamples()).contains(sample);
  }

  private static Clinical calculatePCAWG(Clinical clinical) {
    val calculator = createCalculator(clinical);

    return calculator.calculate();
  }

  private static Clinical createClinical(Record donor, Record specimen, Record sample) {
    return new Clinical(
        new ClinicalCore(of(donor), of(specimen), of(sample)),
        new ClinicalOptional(of(), of(), of(), of(), of()));
  }

  private static PCAWGClinicalCalculator createCalculator(Clinical clinical) {
    val index = new ClinicalIndex(clinical);

    return new PCAWGClinicalCalculator(clinical, index);
  }

}
