package org.icgc.dcc.validation;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
import cascading.pipe.Every;
import cascading.pipe.GroupBy;
import cascading.pipe.Pipe;
import cascading.property.AppProps;
import cascading.scheme.hadoop.TextLine;
import cascading.tap.Tap;
import cascading.tap.hadoop.Hfs;
import cascading.tap.local.FileTap;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

public class Main {
  public static void main(String[] args) {

    Pipe pipe = new Pipe("pipe");
    pipe = new GroupBy(pipe, new Fields("offset"));
    pipe = new Every(pipe, new LineNumberBuffer());
    pipe = new Each(pipe, new AddValidationFieldsFunction(), Fields.ALL);
    pipe = new Each(pipe, new Fields("line", "_errors"), new LineLengthValidator(), Fields.REPLACE);

    Flow<?> flow;
    if(args[0].equals("--local")) {
      flow = makeLocal(args, pipe);
    } else {
      flow = makeHadoop(args, pipe);
    }

    flow.writeDOT("/tmp/dot.dot");

    flow.start();
  }

  private static Flow<?> makeLocal(String[] args, Pipe pipe) {
    FlowConnector connector = new LocalFlowConnector();

    Tap source = new FileTap(new cascading.scheme.local.TextLine(new Fields("offset", "line")), args[1]);
    Tap sink = new FileTap(new cascading.scheme.local.TextLine(), args[2]);

    return connector.connect(source, sink, pipe);
  }

  private static Flow<?> makeHadoop(String[] args, Pipe pipe) {
    Properties props = new Properties();
    AppProps.setApplicationJarClass(props, Main.class);
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
      super(0, new Fields("_errors"));
    }

    @Override
    public void operate(FlowProcess process, FunctionCall functionCall) {
      // create a Tuple to hold our result values
      Tuple result = new Tuple();

      result.add(new ArrayList<String>());

      // return the result Tuple
      functionCall.getOutputCollector().add(result);
    }

  }
}
