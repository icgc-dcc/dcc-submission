package org.icgc.dcc.reporter;

import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.hadoop.cascading.Fields2.checkFieldsCardinalityOne;
import static org.icgc.dcc.hadoop.cascading.Fields2.getCountFieldCounterpart;
import static org.icgc.dcc.hadoop.cascading.Fields2.getFieldNames;
import static org.icgc.dcc.reporter.ReporterFields.DONOR_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.DONOR_UNIQUE_COUNT_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.PROJECT_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.REDUNDANT_PROJECT_ID_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SEQUENCING_STRATEGY_COUNT_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.SEQUENCING_STRATEGY_FIELD;
import static org.icgc.dcc.reporter.ReporterFields.TRANSPOSITION_FIELDS;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import lombok.val;

import org.icgc.dcc.hadoop.cascading.SubAssemblies;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.NullReplacer.NullReplacing;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReadableHashJoin;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.ReadableHashJoin.JoinData;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.UniqueCountBy;
import org.icgc.dcc.hadoop.cascading.SubAssemblies.UniqueCountBy.UniqueCountByData;

import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Buffer;
import cascading.operation.BufferCall;
import cascading.pipe.Every;
import cascading.pipe.GroupBy;
import cascading.pipe.Pipe;
import cascading.pipe.SubAssembly;
import cascading.pipe.assembly.Discard;
import cascading.pipe.assembly.Rename;
import cascading.pipe.assembly.Retain;
import cascading.pipe.assembly.SumBy;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

public class Table2 extends SubAssembly {

  Table2(Pipe preComputationTable, Pipe donors) {
    setTails(table2(preComputationTable, donors));
  }

  private static Pipe table2(Pipe preComputationTable, Pipe donors) {

    return new ReadableHashJoin(JoinData.builder()

        .leftPipe(postProcessDonors(donors))
        .leftJoinFields(REDUNDANT_PROJECT_ID_FIELD)

        .rightPipe(processSequencingStrategies(preComputationTable))
        .rightJoinFields(PROJECT_ID_FIELD)

        .discardFields(REDUNDANT_PROJECT_ID_FIELD)

        .build());
  }

  private static Pipe postProcessDonors(Pipe pipe) {
    pipe = new Retain(pipe, PROJECT_ID_FIELD.append(DONOR_UNIQUE_COUNT_FIELD));
    pipe = new SumBy(pipe, PROJECT_ID_FIELD, DONOR_UNIQUE_COUNT_FIELD, DONOR_UNIQUE_COUNT_FIELD, long.class);
    pipe = new Rename(pipe, PROJECT_ID_FIELD, REDUNDANT_PROJECT_ID_FIELD);
    return pipe;
  }

  private static Pipe processSequencingStrategies(Pipe preComputationTable) {
    Pipe pipe = new UniqueCountBy(UniqueCountByData.builder()

        .pipe(preComputationTable)
        .uniqueFields(
            PROJECT_ID_FIELD
                .append(SEQUENCING_STRATEGY_FIELD)
                .append(DONOR_ID_FIELD))
        .countByFields(
            PROJECT_ID_FIELD
                .append(SEQUENCING_STRATEGY_FIELD))
        .resultCountField(getCountFieldCounterpart(SEQUENCING_STRATEGY_FIELD))

        .build());

    // TODO: refactor as typical
    NullReplacing<String> nullReplacing = new NullReplacing<String>() {

      @Override
      public String get() {
        return "null";
      }

    };
    pipe = new SubAssemblies.NullReplacer<String>(
        SEQUENCING_STRATEGY_FIELD,
        nullReplacing, pipe);

    pipe = new GroupBy(pipe, PROJECT_ID_FIELD, SEQUENCING_STRATEGY_FIELD);
    pipe = new Every(
        pipe,
        new TransposeBuffer<Long>(
            TRANSPOSITION_FIELDS,
            SEQUENCING_STRATEGY_FIELD,
            SEQUENCING_STRATEGY_COUNT_FIELD));
    pipe = new Discard(pipe, SEQUENCING_STRATEGY_FIELD.append(SEQUENCING_STRATEGY_COUNT_FIELD));
    return pipe;
  }

  private static class TransposeBuffer<T> extends BaseOperation<Void> implements Buffer<Void> {

    private final Fields futureFieldsField;
    private final Fields futureValuesField;

    TransposeBuffer(Fields transpositionFields, Fields futureFields, Fields futureValues) {
      super(transpositionFields);
      this.futureFieldsField = futureFields;
      this.futureValuesField = futureValues;
    }

    @Override
    public void operate(
        @SuppressWarnings("rawtypes") FlowProcess flowProcess,
        BufferCall<Void> bufferCall) {
      val entries = bufferCall.getArgumentsIterator();
      bufferCall
          .getOutputCollector()
          .add(transpose(entries));
    }

    private TupleEntry transpose(
        final Iterator<TupleEntry> entries) {
      val counts = new HashMap<String, T>();
      while (entries.hasNext()) {
        val entry = entries.next();
        counts.put(
            checkKeysIntegrity(
                counts,
                entry.getString(futureFieldsField)),
            getT(entry, futureValuesField));
      }

      val transpositionTuple = new Tuple();
      for (val code : getFieldNames(fieldDeclaration)) {
        transpositionTuple.add(
            counts.containsKey(code) ?
                counts.get(code) :
                0L);
      }

      return new TupleEntry(
          fieldDeclaration,
          transpositionTuple);
    }

    @SuppressWarnings("unchecked")
    private T getT(final TupleEntry entry, Fields field) {
      return (T) entry.getObject(checkFieldsCardinalityOne(field));
    }

    private static <T> String checkKeysIntegrity(Map<String, T> counts, String key) {
      checkState(!counts.containsKey(key));
      return key;
    }

  }

}
