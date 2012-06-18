package org.icgc.dcc.validation;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.model.dictionary.Dictionary;
import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.model.dictionary.Restriction;

import cascading.flow.Flow;
import cascading.flow.FlowConnector;
import cascading.flow.FlowProcess;
import cascading.flow.hadoop.HadoopFlowConnector;
import cascading.flow.local.LocalFlowConnector;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.pipe.Each;
import cascading.pipe.Pipe;
import cascading.property.AppProps;
import cascading.scheme.hadoop.TextLine;
import cascading.tap.Tap;
import cascading.tap.hadoop.Hfs;
import cascading.tap.local.FileTap;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.mongodb.BasicDBObject;

public class CopyOfMain {

  static private List<? extends FieldRestrictionFactory> factories = ImmutableList
      .of(new DiscreteValuesFieldRestriction.Factory());

  public static void main(String[] args) throws JsonProcessingException, IOException {

    Dictionary d =
        new ObjectMapper().reader(Dictionary.class).readValue(
            Resources.toString(CopyOfMain.class.getResource("/dictionary.json"), Charsets.UTF_8));

    for(FileSchema fs : d.getFiles()) {
      Pipe pipe = new Pipe(fs.getName());
      pipe = new Each(pipe, new AddValidationFieldsFunction(), Fields.ALL);
      BasicDBObject config = new BasicDBObject();
      config.put("fields", fs.getUniqueFields().toArray(new String[] {}));
      pipe = new UniqueFieldsRestriction.Factory().build(null, config).extend(pipe);
      for(Field f : fs.getFields()) {
        for(Restriction r : f.getRestrictions()) {
          pipe = getFieldRestriction(f, r).extend(pipe);
        }
      }
      Flow<?> flow = makeLocal(args, pipe);
      flow.writeDOT("/tmp/dot.dot");

      flow.start();
    }
  }

  private static PipeExtender getFieldRestriction(Field field, Restriction r) {
    for(FieldRestrictionFactory f : factories) {
      if(f.builds(r.getType())) {
        return f.build(field, r.getConfig());
      }
    }
    return new PipeExtender() {

      @Override
      public Pipe extend(Pipe pipe) {
        return pipe;
      }
    };
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
