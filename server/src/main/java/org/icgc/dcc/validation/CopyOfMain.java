package org.icgc.dcc.validation;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.model.dictionary.Dictionary;
import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.model.dictionary.ValueType;

import cascading.flow.Flow;
import cascading.flow.FlowConnector;
import cascading.flow.FlowProcess;
import cascading.flow.hadoop.HadoopFlowConnector;
import cascading.flow.local.LocalFlowConnector;
import cascading.operation.BaseOperation;
import cascading.operation.Buffer;
import cascading.operation.BufferCall;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.operation.OperationCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.property.AppProps;
import cascading.scheme.hadoop.TextLine;
import cascading.tap.Tap;
import cascading.tap.hadoop.Hfs;
import cascading.tap.local.FileTap;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;

public class CopyOfMain {

  public static void main(String[] args) throws JsonProcessingException, IOException {

    Dictionary d =
        new ObjectMapper().reader(Dictionary.class).readValue(
            Resources.toString(CopyOfMain.class.getResource("/dictionary.json"), Charsets.UTF_8));

    FileSchema studyB = new FileSchema("StudyB");
    studyB.setFields(ImmutableList.<Field> builder()//
        .add(makeField("id", ValueType.INTEGER))//
        .add(makeField("CC", ValueType.TEXT))//
        .add(makeField("snp", ValueType.TEXT))//
        .add(makeField("bmi", ValueType.DECIMAL)).build());

    Pipe pipe = new Pipe(studyB.getName());
    pipe = new Each(pipe, new AddValidationFieldsFunction(), Fields.ALL);
    pipe = new UniqueFieldsRestriction.Factory().build(null, null).extend(pipe);
    DBObject config = new BasicDBObject();
    config.put("values", new String[] { "1", "2" });
    pipe = new DiscreteValuesFieldRestriction.Factory().build(studyB.field("CC").get(), config).extend(pipe);
    pipe = new DiscreteValuesFieldRestriction.Factory().build(studyB.field("snp").get(), config).extend(pipe);

    Flow<?> flow;
    if(args[0].equals("--local")) {
      flow = makeLocal(args, pipe);
    } else {
      flow = makeHadoop(args, pipe);
    }

    flow.writeDOT("/tmp/dot.dot");

    flow.start();
  }

  private static Field makeField(String name, ValueType type) {
    Field f = new Field();
    f.setName(name);
    f.setValueType(type);
    return f;
  }

  private static Flow<?> makeLocal(String[] args, Pipe pipe) {
    FlowConnector connector = new LocalFlowConnector();

    Tap source = new FileTap(new cascading.scheme.local.TextDelimited(true, ","), args[1]);
    Tap sink = new FileTap(new cascading.scheme.local.TextDelimited(true, "\t"), args[2]);

    return connector.connect(source, sink, pipe);
  }

  private static Flow<?> makeHadoop(String[] args, Pipe pipe) {
    Properties props = new Properties();
    AppProps.setApplicationJarClass(props, CopyOfMain.class);
    FlowConnector connector = new HadoopFlowConnector(props);

    Tap source = new Hfs(new TextLine(), args[0]);
    Tap sink = new Hfs(new TextLine(), args[1]);

    return connector.connect(source, sink, pipe);
  }

  public static class LineNumberBuffer extends BaseOperation<LineNumberBuffer.Context> implements
      Buffer<LineNumberBuffer.Context> {

    public static class Context {
      long value = 0;
    }

    public LineNumberBuffer() {
      super(0, new Fields("num"));
    }

    public LineNumberBuffer(Fields fieldDeclaration) {
      super(0, fieldDeclaration);
    }

    @Override
    public void prepare(FlowProcess flowProcess, OperationCall<Context> operationCall) {
      // set the context object, starting at zero
      operationCall.setContext(new Context());
    }

    @Override
    public void operate(FlowProcess flowProcess, BufferCall<Context> bufferCall) {
      bufferCall.getContext().value++;
      Tuple result = new Tuple();
      result.add(bufferCall.getContext().value);
      // We have to iterate on the values, otherwise, the output is null
      TupleEntry e = bufferCall.getArgumentsIterator().next();
      bufferCall.getOutputCollector().add(result);
    }
  }

  public static final class LineLengthValidator extends BaseOperation implements Function {

    LineLengthValidator() {
      super(2, Fields.ARGS);
    }

    @Override
    public void operate(FlowProcess arg0, FunctionCall functionCall) {
      TupleEntry arguments = functionCall.getArguments();
      String line = arguments.getString(0);
      List<String> errors = (List<String>) arguments.getObject(1);
      if(line != null && line.length() > 10) {
        errors.add(String.format("Line is too long: %d", line.length()));
      }
      functionCall.getOutputCollector().add(new Tuple(line, errors));
    }
  }

  public static final class AddValidationFieldsFunction extends BaseOperation implements Function {

    public AddValidationFieldsFunction() {
      super(0, new Fields(ValidationFields.STATE_FIELD_NAME));
    }

    @Override
    public void operate(FlowProcess process, FunctionCall functionCall) {
      // create a Tuple to hold our result values
      Tuple result = new Tuple();

      result.add(new TupleState());

      // return the result Tuple
      functionCall.getOutputCollector().add(result);
    }

  }
}
