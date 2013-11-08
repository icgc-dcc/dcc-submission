/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.normalization.steps;

import static cascading.tuple.Fields.ALL;
import static cascading.tuple.Fields.ARGS;
import static cascading.tuple.Fields.REPLACE;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static org.icgc.dcc.core.model.FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_ANALYSIS_ID;
import static org.icgc.dcc.hadoop.cascading.Fields2.fields;
import static org.icgc.dcc.hadoop.cascading.Fields2.getFieldName;
import static org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter.COUNT_INCREMENT;
import static org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter.DROPPED;
import static org.icgc.dcc.submission.normalization.NormalizationUtils.getFileSchema;
import static org.icgc.dcc.submission.normalization.configuration.ConfigKey.SwitchValue.ENABLED;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.SubmissionFileTypes.SubmissionFileType;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.dictionary.model.FileSchema;
import org.icgc.dcc.submission.normalization.NormalizationContext;
import org.icgc.dcc.submission.normalization.NormalizationReport.NormalizationCounter;
import org.icgc.dcc.submission.normalization.NormalizationStep;
import org.icgc.dcc.submission.normalization.configuration.ConfigurableStep.OptionalStep;
import org.icgc.dcc.submission.normalization.configuration.ConfigKey.SwitchValue;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Buffer;
import cascading.operation.BufferCall;
import cascading.pipe.Every;
import cascading.pipe.GroupBy;
import cascading.pipe.Pipe;
import cascading.tuple.Fields;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;

/**
 * TODO
 */
@RequiredArgsConstructor
@Slf4j
public final class RedundantObservationRemoval implements NormalizationStep, OptionalStep {

  public static final Fields ANALYSIS_ID_FIELD = new Fields(SUBMISSION_OBSERVATION_ANALYSIS_ID);

  /**
   * Name of the field on which to perform the secondary sort.
   */
  private final String secondarySortFieldName;

  /**
   * 
   */
  public static ImmutableList<String> getObservationUniqueFields(Dictionary dictionary, SubmissionFileType type) {
    FileSchema ssmP = getFileSchema(dictionary, type);

    val observationUniqueFields = newArrayList(
        ssmP.getFieldNames());
    observationUniqueFields.remove(
        getFieldName(ANALYSIS_ID_FIELD));
    observationUniqueFields.remove("mutation"); // Hack

    return ImmutableList.<String> copyOf(observationUniqueFields);
  }

  @Override
  public String shortName() {
    return "duplicates";
  }

  @Override
  public boolean isEnabled(Config config) {
    return OptionalSteps.isEnabled(config, this);
  }

  @Override
  public SwitchValue getDefaultSwitchValue() {
    return ENABLED;
  }

  @Override
  public String getOptionalStepKey() {
    return shortName();
  }

  /**
   * Will only emit the first match for a given group, effectively filtering out the others. In the loader component,
   * this will result in rows from the meta file to be discarded as well by virtue of the inner join performed between
   * them.
   */
  @Override
  public Pipe extend(Pipe pipe, NormalizationContext context) {
    pipe =
        new GroupBy(
            pipe,
            groupByFields(context),
            secondarySortFields());

    pipe = new Every(
        pipe,
        ALL,
        new FilterRedundantObservationBuffer(),
        REPLACE);

    pipe = new CountUnique( // Will leave the pipe unaltered
        pipe,
        shortName(),
        ANALYSIS_ID_FIELD,
        NormalizationCounter.UNIQUE_FILTERED,
        COUNT_INCREMENT);

    return pipe;
  }

  /**
   * Returns the {@link Fields} on which the GROUP BY should take place, and that will allow detecting duplicate
   * observations.
   */
  private Fields groupByFields(NormalizationContext context) {
    return fields(context.getObservationUniqueFields());
  }

  private Fields secondarySortFields() {
    return new Fields(secondarySortFieldName);
  }

  @VisibleForTesting
  static final class FilterRedundantObservationBuffer extends BaseOperation<Void> implements Buffer<Void> {

    @VisibleForTesting
    FilterRedundantObservationBuffer() {
      super(ARGS);
    }

    @Override
    public void operate(
        @SuppressWarnings("rawtypes") FlowProcess flowProcess,
        BufferCall<Void> bufferCall) {

      val group = bufferCall.getGroup();
      val tuples = bufferCall.getArgumentsIterator();
      checkState(tuples.hasNext(), "There should always be at least one item for a given group, none for '{}'", group);
      val first = tuples.next().getTupleCopy();

      val duplicates = tuples.hasNext();
      if (duplicates) {
        while (tuples.hasNext()) {
          val duplicate = tuples.next().getTuple();
          log.info("Found a duplicate of '{}' (group '{}'): ", // Should be rare enough an event
              new Object[] { first, group, duplicate });
          flowProcess.increment(DROPPED, COUNT_INCREMENT);
        }
      } else {
        log.debug("No duplicates found for '{}'", group);
      }

      bufferCall
          .getOutputCollector()
          .add(first);
    }
  }

}
