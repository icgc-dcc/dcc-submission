package org.icgc.dcc.validation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.config.ConfigModule;
import org.icgc.dcc.core.morphia.MorphiaModule;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.service.ValidationQueueManagerService;
import org.icgc.dcc.service.ValidationServiceModule;

import cascading.cascade.Cascade;
import cascading.flow.Flow;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.typesafe.config.ConfigFactory;

public class Main {

  private final File root;

  private final File output;

  private final Dictionary dictionary;

  public Main(String[] args) throws JsonProcessingException, IOException {
    this.root = new File(args[0]);
    this.output = new File(args[1]);
    this.output.mkdirs();
    this.dictionary =
        new ObjectMapper().reader(Dictionary.class).readValue(
            Resources.toString(Main.class.getResource("/dictionary.json"), Charsets.UTF_8));
  }

  public static void main(String[] args) throws JsonProcessingException, IOException {
    new Main(args).doit();
  }

  @SuppressWarnings("rawtypes")
  private void doit() {
    Injector injector = Guice.createInjector(new ValidationModule(root, output),//
        new ConfigModule(ConfigFactory.load()),//
        new MorphiaModule(),//
        new ValidationServiceModule()//
        );

    Planner planner = injector.getInstance(Planner.class);
    Plan plan = planner.plan(injector.getInstance(FileSchemaDirectory.class), dictionary);

    Cascade cascade = plan.connect(injector.getInstance(CascadingStrategy.class));
    cascade.writeDOT(new File(output, "cascade.dot").getAbsolutePath());

    List<Flow> flows = cascade.getFlows();
    for(Flow flow : flows) {
      flow.writeDOT(new File(output, flow.getName() + ".dot").getAbsolutePath());
    }

    ValidationCallback callback = injector.getInstance(ValidationQueueManagerService.class);
    for(Flow flow : flows) {
      String projectKey = plan.toString();// TODO: actually get project key
      ValidationFlowListener listener = new ValidationFlowListener(callback, flows, projectKey);
      flow.addListener(listener);
    }
    if(flows.size() > 0) {
      cascade.start();
    }
  }
}
