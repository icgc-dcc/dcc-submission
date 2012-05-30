package org.icgc.dcc.validation;

import java.util.ArrayList;
import java.util.List;

import cascading.flow.Flow;
import cascading.flow.FlowProcess;
import cascading.flow.local.LocalFlowConnector;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.scheme.local.TextLine;
import cascading.tap.local.FileTap;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;
import cascading.tuple.TupleEntry;

public class Main {
  public static void main(String[] args) {

    Pipe pipe = new Pipe("pipe");
    pipe = new Each(pipe, new AddValidationFieldsFunction(), Fields.ALL);
    pipe = new Each(pipe, new Fields("line", "num", "_errors"), new LineLengthValidator(), Fields.ALL);

    FileTap source = new FileTap(new TextLine(), args[0]);
    FileTap sink = new FileTap(new TextLine(), args[1]);

    LocalFlowConnector connector = new LocalFlowConnector();

    Flow<?> flow = connector.connect(source, sink, pipe);

    flow.writeDOT("/tmp/dot.dot");

    flow.start();

  }

  public static final class LineLengthValidator extends BaseOperation implements Function {

    LineLengthValidator() {
      super(3);
    }

    @Override
    public void operate(FlowProcess arg0, FunctionCall functionCall) {
      TupleEntry arguments = functionCall.getArguments();
      if(arguments.getString(0).length() > 10) {
        List<String> errors = (List<String>) arguments.getObject(2);
        errors.add(String.format("Line %d is too long: %d", arguments.getInteger(1), arguments.getString(0).length()));
      }
      functionCall.getOutputCollector().add(new Tuple());
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
