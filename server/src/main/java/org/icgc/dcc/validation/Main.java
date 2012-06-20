package org.icgc.dcc.validation;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.model.dictionary.Dictionary;
import org.icgc.dcc.model.dictionary.Field;
import org.icgc.dcc.model.dictionary.FileSchema;
import org.icgc.dcc.model.dictionary.Restriction;
import org.icgc.dcc.validation.cascading.TupleState;
import org.icgc.dcc.validation.cascading.ValidationFields;
import org.icgc.dcc.validation.plan.DefaultPlan;
import org.icgc.dcc.validation.plan.FileSchemaPlan;
import org.icgc.dcc.validation.restriction.DiscreteValuesPipeExtender;
import org.icgc.dcc.validation.restriction.ForeingKeyFieldRestriction;

import cascading.cascade.Cascade;
import cascading.flow.FlowProcess;
import cascading.operation.BaseOperation;
import cascading.operation.Function;
import cascading.operation.FunctionCall;
import cascading.tuple.Fields;
import cascading.tuple.Tuple;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

public class Main {

  static private List<? extends RestrictionType> factories = ImmutableList.of(new DiscreteValuesPipeExtender.Type(),
      new ForeingKeyFieldRestriction.Type());

  private final File root;

  private final File output;

  private final Dictionary dictionary;

  public Main(String[] args) throws JsonProcessingException, IOException {
    this.root = new File(args[0]);
    this.output = new File(args[1]);
    this.dictionary =
        new ObjectMapper().reader(Dictionary.class).readValue(
            Resources.toString(Main.class.getResource("/dictionary.json"), Charsets.UTF_8));
  }

  public static void main(String[] args) throws JsonProcessingException, IOException {
    new Main(args).doit();
  }

  private void doit() {
    for(File f : output.listFiles()) {
      if(f.isFile()) {
        f.delete();
      }
    }

    DefaultPlan dp = new DefaultPlan();
    for(FileSchema fs : dictionary.getFiles()) {
      if(hasFile(fs)) {
        dp.prepare(fs);
      }
    }

    for(FileSchemaPlan fsPlan : dp.getSchemaPlans()) {
      for(Field f : fsPlan.getSchema().getFields()) {
        for(Restriction r : f.getRestrictions()) {
          applyFieldRestriction(fsPlan, f, r);
        }
      }
    }

    Cascade c = dp.plan(root, output);
    c.writeDOT("/tmp/dot.dot");
    c.start();
  }

  private boolean hasFile(final FileSchema fs) {
    File[] files = root.listFiles(new FileFilter() {

      @Override
      public boolean accept(File pathname) {
        return pathname.getName().contains(fs.getName());
        // return Pattern.matches(fs.getPattern(), pathname.getName());
      }
    });
    return files != null && files.length > 0;
  }

  private static void applyFieldRestriction(FileSchemaPlan plan, Field field, Restriction restriction) {
    for(RestrictionType type : factories) {
      if(type.builds(restriction.getType())) {
        type.apply(plan, field, restriction);
      }
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
