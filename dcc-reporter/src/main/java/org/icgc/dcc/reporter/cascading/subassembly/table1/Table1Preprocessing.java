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
package org.icgc.dcc.reporter.cascading.subassembly.table1;

import static cascading.tuple.Fields.NONE;
import static com.google.common.collect.Iterables.toArray;
import static org.icgc.dcc.reporter.OutputType.DONOR;
import static org.icgc.dcc.reporter.OutputType.OBSERVATION;
import static org.icgc.dcc.reporter.OutputType.SAMPLE;
import static org.icgc.dcc.reporter.OutputType.SPECIMEN;
import static org.icgc.dcc.reporter.ReporterFields.COUNT_BY_FIELDS;
import static org.icgc.dcc.reporter.ReporterFields.DONOR_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SAMPLE_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SPECIMEN_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.TYPE_FIELD;
import static org.icgc.dcc.reporter.ReporterFields._ANALYSIS_OBSERVATION_COUNT_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.getTemporaryCountByFields;

import org.icgc.dcc.hadoop.cascading.SubAssemblies.NamingPipe;
import org.icgc.dcc.reporter.cascading.subassembly.ProcessClinicalType;

import cascading.pipe.HashJoin;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Rename;
import cascading.pipe.assembly.Retain;
import cascading.pipe.assembly.SumBy;
import cascading.pipe.joiner.InnerJoin;
import cascading.tuple.Fields;

import com.google.common.collect.ImmutableList;

public class Table1Preprocessing extends SubAssembly {

  Table1Preprocessing(Pipe preComputationTable) {
    setTails(process(preComputationTable));
  }

  private static Pipe process(Pipe preComputationTable) {
    return new Rename(
        new Retain(
            new HashJoin(
                // TODO: create more readable subassembly
                toArray(
                    ImmutableList.<Pipe> builder()

                        .add(ProcessClinicalType.donor(preComputationTable))
                        .add(ProcessClinicalType.specimen(preComputationTable))
                        .add(ProcessClinicalType.sample(preComputationTable))
                        .add(processObservations(preComputationTable))

                        .build()
                    , Pipe.class),
                new Fields[] {
                    getTemporaryCountByFields(DONOR),
                    getTemporaryCountByFields(SPECIMEN),
                    getTemporaryCountByFields(SAMPLE),
                    getTemporaryCountByFields(OBSERVATION) },
                NONE
                    .append(DONOR_UNIQUE_COUNT_FIELD).append(getTemporaryCountByFields(DONOR))
                    .append(SPECIMEN_UNIQUE_COUNT_FIELD.append(getTemporaryCountByFields(SPECIMEN)))
                    .append(SAMPLE_UNIQUE_COUNT_FIELD.append(getTemporaryCountByFields(SAMPLE)))
                    .append(_ANALYSIS_OBSERVATION_COUNT_FIELD.append(getTemporaryCountByFields(OBSERVATION))),
                new InnerJoin()),
            getTemporaryCountByFields(DONOR)
                .append(DONOR_UNIQUE_COUNT_FIELD)
                .append(SPECIMEN_UNIQUE_COUNT_FIELD)
                .append(SAMPLE_UNIQUE_COUNT_FIELD)
                .append(_ANALYSIS_OBSERVATION_COUNT_FIELD)),
        getTemporaryCountByFields(DONOR),
        PROJECT_ID_FIELD.append(TYPE_FIELD));
  }

  private static Pipe processObservations(Pipe preComputationTable) {
    return new NamingPipe(
        OBSERVATION,

        new Rename(

            new SumBy( // TODO: retain necessary?
                preComputationTable,
                COUNT_BY_FIELDS,
                _ANALYSIS_OBSERVATION_COUNT_FIELD,
                _ANALYSIS_OBSERVATION_COUNT_FIELD,
                long.class),
            COUNT_BY_FIELDS,
            getTemporaryCountByFields(OBSERVATION)));
  }

}
