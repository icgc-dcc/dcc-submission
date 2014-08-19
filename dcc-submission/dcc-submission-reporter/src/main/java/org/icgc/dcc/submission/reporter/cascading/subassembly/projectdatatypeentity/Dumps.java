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

import static cascading.tuple.Fields.NONE;
import static org.icgc.dcc.submission.reporter.ReporterFields.ANALYSIS_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.DONOR_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.DONOR_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.RELEASE_NAME_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SAMPLE_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SAMPLE_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SEQUENCING_STRATEGY_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SPECIMEN_ID_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.SPECIMEN_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields.TYPE_FIELD;
import static org.icgc.dcc.submission.reporter.ReporterFields._ANALYSIS_OBSERVATION_COUNT_FIELD;
import lombok.NonNull;

import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReorderFields;
import org.icgc.dcc.submission.reporter.OutputType;

import cascading.pipe.Pipe;

/**
 * TODO: very ugly, must address ASAP!
 */
public class Dumps {

  public static Pipe preComputation(
      @NonNull final String projectKey,
      @NonNull final Pipe pipe) {
    return new ReorderFields(
        new Pipe(OutputType.PRE_COMPUTATION + projectKey, pipe),
        NONE.append(RELEASE_NAME_FIELD)
            .append(PROJECT_ID_FIELD)
            .append(TYPE_FIELD)
            .append(DONOR_ID_FIELD)
            .append(SPECIMEN_ID_FIELD)
            .append(SAMPLE_ID_FIELD)
            .append(ANALYSIS_ID_FIELD)
            .append(SEQUENCING_STRATEGY_FIELD)
            .append(_ANALYSIS_OBSERVATION_COUNT_FIELD));
  }

  static Pipe preProcessingAll(
      @NonNull final String projectKey,
      @NonNull final Pipe pipe) {
    return new ReorderFields(
        new Pipe(OutputType.PRE_PROCESSING_ALL + projectKey, pipe),
        NONE.append(PROJECT_ID_FIELD)
            .append(DONOR_UNIQUE_COUNT_FIELD)
            .append(SPECIMEN_UNIQUE_COUNT_FIELD)
            .append(SAMPLE_UNIQUE_COUNT_FIELD)
            .append(_ANALYSIS_OBSERVATION_COUNT_FIELD));
  }

  static Pipe preProcessingFeatureTypes(
      @NonNull final String projectKey,
      @NonNull final Pipe pipe) {
    return new ReorderFields(
        new Pipe(OutputType.PRE_PROCESSING_FEATURE_TYPES + projectKey, pipe),
        NONE.append(PROJECT_ID_FIELD)
            .append(TYPE_FIELD)
            .append(DONOR_UNIQUE_COUNT_FIELD)
            .append(SPECIMEN_UNIQUE_COUNT_FIELD)
            .append(SAMPLE_UNIQUE_COUNT_FIELD)
            .append(_ANALYSIS_OBSERVATION_COUNT_FIELD));
  }

}
