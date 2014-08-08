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
package org.icgc.dcc.submission.reporter.cascading.subassembly;

import static org.icgc.dcc.hadoop.cascading.Fields2.checkFieldsCardinalityOne;
import static org.icgc.dcc.hadoop.cascading.Fields2.getCountFieldCounterpart;
import static org.icgc.dcc.submission.reporter.OutputType.DONOR;
import static org.icgc.dcc.submission.reporter.OutputType.SAMPLE;
import static org.icgc.dcc.submission.reporter.OutputType.SPECIMEN;
import static org.icgc.dcc.submission.reporter.ReporterFields.COUNT_BY_FIELDS;
import static org.icgc.dcc.submission.reporter.ReporterFields.DONOR_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SAMPLE_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SPECIMEN_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.getTemporaryCountByFields;
import lombok.NonNull;

import org.icgc.dcc.hadoop.cascading.SubAssemblies.NamingPipe;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.UniqueCountBy;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.UniqueCountBy.UniqueCountByData;
import org.icgc.dcc.submission.reporter.OutputType;

import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Rename;
import cascading.tuple.Fields;

public class ProcessClinicalType extends SubAssembly {

  public ProcessClinicalType(Pipe preComputationTable, OutputType outputType, Fields f) {
    setTails(process(preComputationTable, outputType, f));
  }

  public static Pipe process(Pipe preComputationTable, OutputType outputType, Fields clinicalIdField) {
    checkFieldsCardinalityOne(clinicalIdField);

    return new NamingPipe(
        outputType,

        new Rename(
            new UniqueCountBy(UniqueCountByData.builder()

                .pipe(preComputationTable)
                .uniqueFields(COUNT_BY_FIELDS.append(clinicalIdField))
                .countByFields(COUNT_BY_FIELDS)
                .resultCountField(getCountFieldCounterpart(clinicalIdField))

                .build()),
            COUNT_BY_FIELDS,
            getTemporaryCountByFields(outputType)));
  }

  public static ProcessClinicalType donor(@NonNull final Pipe preComputationTable) {
    return new ProcessClinicalType(
        preComputationTable,
        DONOR,
        DONOR_ID_FIELD);
  }

  public static ProcessClinicalType specimen(@NonNull final Pipe preComputationTable) {
    return new ProcessClinicalType(
        preComputationTable,
        SPECIMEN,
        SPECIMEN_ID_FIELD);
  }

  public static ProcessClinicalType sample(@NonNull final Pipe preComputationTable) {
    return new ProcessClinicalType(
        preComputationTable,
        SAMPLE,
        SAMPLE_ID_FIELD);
  }

}
