package org.icgc.dcc.reporter;

import static cascading.tuple.Fields.ARGS;
import static cascading.tuple.Fields.REPLACE;
import static com.google.common.collect.Iterables.toArray;
import static org.icgc.dcc.reporter.OutputType.DONOR;
import static org.icgc.dcc.reporter.OutputType.OBSERVATION;
import static org.icgc.dcc.reporter.OutputType.SAMPLE;
import static org.icgc.dcc.reporter.OutputType.SPECIMEN;
import static org.icgc.dcc.reporter.PreComputation.ANALYSIS_ID;
import static org.icgc.dcc.reporter.PreComputation.ANALYSIS_OBSERVATION_COUNT;
import static org.icgc.dcc.reporter.PreComputation.DONOR_ID;
import static org.icgc.dcc.reporter.PreComputation.PROJECT_ID;
import static org.icgc.dcc.reporter.PreComputation.SAMPLE_ID;
import static org.icgc.dcc.reporter.PreComputation.SPECIMEN_ID;
import static org.icgc.dcc.reporter.PreComputation.TYPE;
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

public class StatsGathering extends SubAssembly {

  StatsGathering(Pipe preComputationTable) {
    setTails(toArray(
        process(preComputationTable),
        Pipe.class));
  }

  public Iterable<Pipe> process(Pipe preComputationTable) {
    return ImmutableList.<Pipe> builder()
        .add(new Pipe(DONOR.name(), getUniqueCountPipe(preComputationTable, DONOR_ID, PROJECT_ID, TYPE)))
        .add(new Pipe(SPECIMEN.name(), getUniqueCountPipe(preComputationTable, SPECIMEN_ID, PROJECT_ID, TYPE)))
        .add(new Pipe(SAMPLE.name(), getUniqueCountPipe(preComputationTable, SAMPLE_ID, PROJECT_ID, TYPE)))
        .add(new Pipe(OBSERVATION.name(), getPreCountedUniqueCountPipe(preComputationTable, ANALYSIS_ID,
            ANALYSIS_OBSERVATION_COUNT, PROJECT_ID, TYPE)))
        .add(
            new Pipe(
                PreComputation.SEQUENCING_STRATEGY,
                getCountPipe(preComputationTable, ANALYSIS_ID, PROJECT_ID, PreComputation.SEQUENCING_STRATEGY)))
        .build();
  }

  private Pipe getCountPipe(Pipe preComputationTable, String targetFieldName, String... groupFieldNames) {
    return

    //
    new CountBy(

        //
        new Retain(
            preComputationTable,

            //
            new Fields(groupFieldNames)
                .append(new Fields(targetFieldName))),

        //
        new Fields(groupFieldNames),

        //
        new Fields(targetFieldName + "_count"));
  }

  // see https://gist.github.com/ceteri/4459908
  private Pipe getUniqueCountPipe(Pipe preComputationTable, String targetFieldName, String... groupFieldNames) {
    return

    //
    new CountBy(

        //
        new Unique( // TODO: automatically retains?
            preComputationTable,

            //
            new Fields(groupFieldNames)
                .append(new Fields(targetFieldName))),

        //
        new Fields(groupFieldNames),

        //
        new Fields(targetFieldName + "_count"));
  }

  private Pipe getPreCountedUniqueCountPipe(
      Pipe preComputationTable, String targetFieldName, String preCountFieldName, String... groupFieldNames) {

    class MyBuffer extends BaseOperation<Void> implements Buffer<Void> {

      public MyBuffer() {
        super(ARGS);
      }

      @Override
      public void operate(@SuppressWarnings("rawtypes") FlowProcess flowProcess, BufferCall<Void> bufferCall) {
        long observationCount = 0;
        val entries = bufferCall.getArgumentsIterator();
        while (entries.hasNext()) {
          observationCount += entries.next().getInteger(0); // TODO: use TupleEntries utility when merged
        }
        bufferCall.getOutputCollector().add(new Tuple(observationCount));
      }
    }

    return

    //
    new Retain(

        //
        new Every(

            //
            new GroupBy(

                //
                new Unique(
                    preComputationTable,

                    //
                    new Fields(groupFieldNames)
                        .append(new Fields(targetFieldName, preCountFieldName))),

                //
                new Fields(groupFieldNames)),

            //
            new Fields(ANALYSIS_OBSERVATION_COUNT),

            //
            new MyBuffer(),

            REPLACE),

        //
        new Fields(groupFieldNames)
            .append(new Fields(ANALYSIS_OBSERVATION_COUNT)));
  }
}
