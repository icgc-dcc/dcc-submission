package org.icgc.dcc.validation;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.config.ConfigModule;
import org.icgc.dcc.core.morphia.MorphiaModule;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.service.ValidationServiceModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cascading.cascade.Cascade;
import cascading.flow.Flow;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.typesafe.config.ConfigFactory;

public class Main {
  private static final Logger log = LoggerFactory.getLogger(Main.class);

  private final File root;

  private final File output;

  private final Dictionary dictionary;

  public static void main(String[] args) throws JsonProcessingException, IOException {
    File root = new File(args[0]);
    File output = new File(args[1]);
    new Main(root, output).run();
  }

  public Main(File root, File output) {
    this.root = root;
    this.output = output;
    this.output.mkdirs();
    try {
      this.dictionary =
          new ObjectMapper().reader(Dictionary.class).readValue(
              Resources.toString(Main.class.getResource("/dictionary.json"), Charsets.UTF_8));
    } catch(Exception e) {
      throw new RuntimeException();// TODO: proper exception
    }
  }

  private void run() {
    if(output.exists() && output.listFiles() != null) {
      for(File f : output.listFiles()) {
        if(f.isFile()) {
          f.delete();
        }
      }
    }

    // TODO: DCC-119 to give the submission name properly to main class
    String projectKey = FilenameUtils.getBaseName(this.root.getAbsolutePath());
    log.info("projectKey = {}", projectKey);

    Injector injector = Guice.createInjector(new ConfigModule(ConfigFactory.load())//
        , new ValidationModule(root, output)//
        , new MorphiaModule()//
        , new ValidationServiceModule()//
        );

    Planner planner = injector.getInstance(Planner.class);
    FileSchemaDirectory fileSchemaDirectory = injector.getInstance(FileSchemaDirectory.class);
    CascadingStrategy cascadingStrategy = injector.getInstance(CascadingStrategy.class);

    process(projectKey, planner, fileSchemaDirectory, cascadingStrategy, null);
  }

  @SuppressWarnings("rawtypes")
  public void process(String projectKey, Planner planner, FileSchemaDirectory fileSchemaDirectory,
      CascadingStrategy cascadingStrategy, ValidationCallback successCallback) {
    log.info("root = {} ", root);
    log.info("output = {} ", output);

    Plan plan = planner.plan(fileSchemaDirectory, dictionary);
    log.info("# internal flows: {}", Iterables.toArray(plan.getInternalFlows(), InternalFlowPlanner.class).length);
    log.info("# external flows: {}", Iterables.toArray(plan.getExternalFlows(), ExternalFlowPlanner.class).length);

    Cascade cascade = plan.connect(cascadingStrategy);
    cascade.writeDOT(new File(output, "cascade.dot").getAbsolutePath());

    List<Flow> flows = cascade.getFlows();
    for(Flow flow : flows) {
      flow.writeDOT(new File(output, flow.getName() + ".dot").getAbsolutePath());
    }

    for(Flow flow : flows) {
      ValidationFlowListener listener = new ValidationFlowListener(successCallback, flows, projectKey);
      flow.addListener(listener);// TODO: once a cascade listener is available, use it instead
    }
    if(flows.size() > 0) {
      log.info("starting cascased with {} flows", cascade.getFlows().size());
      cascade.start();
    } else {
      log.info("no flows to run");
    }
  }
}
