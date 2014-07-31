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

import static com.google.common.collect.Iterables.toArray;
import static com.google.common.collect.Iterables.transform;
import static org.icgc.dcc.core.model.ClinicalType.CLINICAL;
import static org.icgc.dcc.core.model.ClinicalType.CLINICAL_CORE_TYPE;
import static org.icgc.dcc.hadoop.cascading.Fields2.checkFieldsCardinalityOne;
import static org.icgc.dcc.hadoop.cascading.Fields2.keyValuePair;
import static org.icgc.dcc.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.TABLE1_COUNT_FIELDS;
import static org.icgc.dcc.reporter.ReporterFields.TABLE1_RESULT_FIELDS;
import static org.icgc.dcc.reporter.ReporterFields.TYPE_FIELD;
import lombok.NonNull;
import lombok.val;

import org.icgc.dcc.hadoop.cascading.SubAssemblies;

import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.AggregateBy;
import cascading.pipe.assembly.SumBy;
import cascading.tuple.Fields;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;

public class Table1ClinicalProcessing extends SubAssembly {

  Table1ClinicalProcessing(@NonNull final Pipe preComputationTable) {
    setTails(process(preComputationTable));
  }

  private static Pipe process(@NonNull final Pipe preComputationTable) {
    val clinicalPipe = new Pipe(
        CLINICAL_CORE_TYPE.getTypeName(),
        preComputationTable);

    return new SubAssemblies.ReorderFields(
        new SubAssemblies.Insert(
            keyValuePair(
                TYPE_FIELD,
                CLINICAL),
            new AggregateBy(
                clinicalPipe,
                PROJECT_ID_FIELD,
                toArray(
                    getSumBys(clinicalPipe),
                    AggregateBy.class))),
        TABLE1_RESULT_FIELDS);
  }

  private static Iterable<AggregateBy> getSumBys(@NonNull final Pipe clinicalPipe) {
    return ImmutableList.copyOf(transform(
        TABLE1_COUNT_FIELDS,
        new Function<Fields, AggregateBy>() {

          @Override
          public AggregateBy apply(Fields countField) {
            return new SumBy(
                clinicalPipe,
                PROJECT_ID_FIELD,
                checkFieldsCardinalityOne(countField),
                countField,
                long.class);
          }

        }));
  }

}
