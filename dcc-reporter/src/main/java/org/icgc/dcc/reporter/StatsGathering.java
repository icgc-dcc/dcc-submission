package org.icgc.dcc.reporter;

import static com.google.common.collect.Iterables.toArray;
import static org.icgc.dcc.reporter.PreComputation.ANALYSIS_ID;
import static org.icgc.dcc.reporter.PreComputation.ANALYSIS_OBSERVATION_COUNT;
import static org.icgc.dcc.reporter.PreComputation.DONOR_ID;
import static org.icgc.dcc.reporter.PreComputation.PROJECT_ID;
import static org.icgc.dcc.reporter.PreComputation.SAMPLE_ID;
import static org.icgc.dcc.reporter.PreComputation.SEQUENCING_STRATEGY;
import static org.icgc.dcc.reporter.PreComputation.SPECIMEN_ID;
import static org.icgc.dcc.reporter.PreComputation.TYPE;

import java.util.Map;

import lombok.Getter;
import lombok.val;
import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Buffer;
import cascading.operation.BufferCall;
import cascading.pipe.Every;
import cascading.pipe.GroupBy;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.CountBy;
import cascading.pipe.assembly.Retain;
import cascading.pipe.assembly.Unique;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

public class StatsGathering extends SubAssembly {

  StatsGathering(Pipe preComputationTable) {
    setTails(toArray(
        process(preComputationTable),
        Pipe.class));
  }

  @Getter
  // TODO: as immutable
  private final Map<OutputType, Pipe> outputPipes = Maps.newLinkedHashMap();

  public Iterable<Pipe> process(Pipe preComputationTable) {
    val donorCountPipe = new Pipe("donor", getUniqueCountPipe(preComputationTable, DONOR_ID, PROJECT_ID, TYPE));
    outputPipes.put(OutputType.DONOR, donorCountPipe);

    val specimenCountPipe =
        new Pipe("specimen", getUniqueCountPipe(preComputationTable, SPECIMEN_ID, PROJECT_ID, TYPE));
    outputPipes.put(OutputType.SPECIMEN, specimenCountPipe);

    val sampleCountPipe = new Pipe("sample", getUniqueCountPipe(preComputationTable, SAMPLE_ID, PROJECT_ID, TYPE));
    outputPipes.put(OutputType.SAMPLE, sampleCountPipe);

    // Observation count has been precounted
    val observationCountPipe =
        new Pipe("observation", getPreCountedUniqueCountPipe(preComputationTable, ANALYSIS_ID,
            ANALYSIS_OBSERVATION_COUNT, PROJECT_ID, TYPE));
    outputPipes.put(OutputType.OBSERVATION, observationCountPipe);

    // Does not include the type
    val sequencingStrategyCountPipe =
        new Pipe(SEQUENCING_STRATEGY, getCountPipe(preComputationTable, ANALYSIS_ID, PROJECT_ID, SEQUENCING_STRATEGY));
    outputPipes.put(OutputType.SEQUENCING_STRATEGY, sequencingStrategyCountPipe);

    return ImmutableList.<Pipe> builder()
        .add(donorCountPipe)
        .add(specimenCountPipe)
        .add(sampleCountPipe)
        .add(observationCountPipe)
        .add(sequencingStrategyCountPipe)
        .build();
  }

  private Pipe getCountPipe(Pipe projects, String targetFieldName, String... groupFieldNames) {
    return new CountBy(
        new Retain(
            projects,
            new Fields(groupFieldNames)
                .append(new Fields(targetFieldName))),
        new Fields(groupFieldNames),
        new Fields(targetFieldName + "_count"));
  }

  // see https://gist.github.com/ceteri/4459908
  private Pipe getUniqueCountPipe(Pipe projects, String targetFieldName, String... groupFieldNames) {
    return new CountBy(
        new Unique( // TODO: automatically retains?
            projects,
            new Fields(groupFieldNames)
                .append(new Fields(targetFieldName))),
        new Fields(groupFieldNames),
        new Fields(targetFieldName + "_count"));
  }

  private Pipe getPreCountedUniqueCountPipe(Pipe projects, String targetFieldName, String preCountFieldName,
      String... groupFieldNames) {

    // @SuppressWarnings("rawtypes")
    class MyBuffer extends BaseOperation<Void> implements Buffer<Void> { // TODO: use aggregator rather?

      public MyBuffer() {
        super(3, new Fields("_observation_count"));
      }

      @Override
      public void operate(@SuppressWarnings("rawtypes") FlowProcess flowProcess, BufferCall<Void> bufferCall) {
        long observationCount = 0;
        val entries = bufferCall.getArgumentsIterator();
        while (entries.hasNext()) {
          observationCount += entries.next().getInteger(ANALYSIS_OBSERVATION_COUNT);
        }
        bufferCall.getOutputCollector().add(new Tuple(observationCount));
      }
    }

    return new Retain(
        new Every(
            new GroupBy(
                new Unique(
                    projects,
                    new Fields(groupFieldNames)
                        .append(new Fields(targetFieldName, preCountFieldName))),
                new Fields(groupFieldNames)),
            new MyBuffer()),
        new Fields(groupFieldNames)
            .append(new Fields("_observation_count")));
  }

}
