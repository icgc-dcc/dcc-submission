/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.reporter.cascading.subassembly.projectdatatypeentity;

import static org.icgc.dcc.submission.reporter.OutputType.OBSERVATION;
import static org.icgc.dcc.submission.reporter.ReporterFields._ANALYSIS_OBSERVATION_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.getTemporaryCountByFields;
import lombok.NonNull;

import org.icgc.dcc.hadoop.cascading.SubAssemblies.NamingPipe;

import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Rename;
import cascading.pipe.assembly.SumBy;
import cascading.tuple.Fields;

public class ObservationCounts extends SubAssembly {

  static Pipe observations(
      @NonNull final Pipe preComputationTable,
      @NonNull final Fields countByFields) {

    return new ObservationCounts(preComputationTable, countByFields);
  }

  private ObservationCounts(
      @NonNull final Pipe preComputationTable,
      @NonNull final Fields countByFields) {
    setTails(process(preComputationTable, countByFields));
  }

  private static Pipe process(
      @NonNull final Pipe preComputationTable,
      @NonNull final Fields countByFields) {
    return new NamingPipe(
        OBSERVATION,

        new Rename(

            new SumBy(
                preComputationTable,
                countByFields,
                _ANALYSIS_OBSERVATION_COUNT_FIELD,
                _ANALYSIS_OBSERVATION_COUNT_FIELD,
                long.class),
            countByFields,
            getTemporaryCountByFields(countByFields, OBSERVATION)));
  }

}
