package org.icgc.dcc.validation;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.map.ObjectMapper;
import org.icgc.dcc.config.ConfigModule;
import org.icgc.dcc.core.morphia.MorphiaModule;
import org.icgc.dcc.dictionary.model.Dictionary;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.FileSystemModule;
import org.icgc.dcc.service.ValidationModule;
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

public class Validator {
  private static final Logger log = LoggerFactory.getLogger(Validator.class);

  private final Planner planner;

  private final ValidationCallback successCallback;

  private final FileSchemaDirectory fileSchemaDirectory;

  private final CascadingStrategy cascadingStrategy;

  private final Dictionary dictionary;

  private final String projectKey;

  public static void main(String[] args) throws Exception {
    String dictionaryFilePath = args[0];
    String rootDirPath = args[1];
    checkArgument(dictionaryFilePath != null);
    checkArgument(rootDirPath != null);

    File root = new File(rootDirPath);
    File output = new File(rootDirPath + Path.SEPARATOR_CHAR + DccFileSystem.VALIDATION_DIRNAME);
    output.mkdirs();
    Dictionary dictionary =
        new ObjectMapper().reader(Dictionary.class).readValue(
            Resources.toString(Validator.class.getResource("/" + dictionaryFilePath), Charsets.UTF_8));

    log.info("dictionaryFilePath = {} ", dictionaryFilePath);
    log.info("root = {} ", root);
    log.info("output = {} ", output);

    Injector injector = Guice.createInjector(new ConfigModule(ConfigFactory.load())//
        , new FileSystemModule()//
        , new MorphiaModule()//
        , new ValidationModule()//
        );
    Planner planner = injector.getInstance(Planner.class);
    FileSchemaDirectory fileSchemaDirectory = new LocalFileSchemaDirectory(root);
    CascadingStrategy cascadingStrategy = new LocalCascadingStrategy(root, output);
    new Validator(planner, fileSchemaDirectory, cascadingStrategy, dictionary).validate();
  }

  public Validator(final Planner planner, final FileSchemaDirectory fileSchemaDirectory,
      final CascadingStrategy cascadingStrategy, final Dictionary dictionary, final ValidationCallback successCallback,
      final String projectKey) {

    checkArgument(planner != null);
    checkArgument(fileSchemaDirectory != null);
    checkArgument(cascadingStrategy != null);
    checkArgument(dictionary != null);

    this.planner = planner;
    this.fileSchemaDirectory = fileSchemaDirectory;
    this.cascadingStrategy = cascadingStrategy;
    this.dictionary = dictionary;
    this.successCallback = successCallback; // may be null
    this.projectKey = projectKey; // may be null (only matters for callback)
  }

  public Validator(final Planner planner, final FileSchemaDirectory fileSchemaDirectory,
      final CascadingStrategy cascadingStrategy, final Dictionary dictionary) {
    this(planner, fileSchemaDirectory, cascadingStrategy, dictionary, null, null);
  }

  @SuppressWarnings("rawtypes")
  public void validate() {

    Plan plan = planner.plan(fileSchemaDirectory, dictionary);
    log.info("# internal flows: {}", Iterables.toArray(plan.getInternalFlows(), InternalFlowPlanner.class).length);
    log.info("# external flows: {}", Iterables.toArray(plan.getExternalFlows(), ExternalFlowPlanner.class).length);

    Cascade cascade = plan.connect(cascadingStrategy);
    List<Flow> flows = cascade.getFlows();
    if(successCallback != null) {
      for(Flow flow : flows) {
        ValidationFlowListener listener = new ValidationFlowListener(successCallback, flows, projectKey);
        flow.addListener(listener);// TODO: once a cascade listener is available, use it instead
      }
    }
    if(flows.size() > 0) {
      log.info("starting cascased with {} flows", cascade.getFlows().size());
      cascade.start();
    } else {
      log.info("no flows to run");
    }
  }
}
