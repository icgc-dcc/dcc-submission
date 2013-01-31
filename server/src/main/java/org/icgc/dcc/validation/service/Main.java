package org.icgc.dcc.validation.service;

import static com.google.common.base.Preconditions.checkArgument;

import org.icgc.dcc.config.ConfigModule;
import org.icgc.dcc.core.morphia.MorphiaModule;
import org.icgc.dcc.filesystem.FileSystemModule;
import org.icgc.dcc.release.CompletedRelease;
import org.icgc.dcc.release.NextRelease;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.QueuedProject;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.validation.Plan;
import org.icgc.dcc.validation.ValidationModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.typesafe.config.ConfigFactory;

public class Main {

  private static final Logger log = LoggerFactory.getLogger(Main.class);

  private static final String HADOOP_USER_NAME_PARAM = "HADOOP_USER_NAME";

  private static final String HADOOP_USER_NAME = "hdfs";

  public static enum CONFIG {
    qa("application_qa"), dev("application_dev"), local("application");

    public String filename;

    private CONFIG(String filename) {
      this.filename = filename;
    }
  }

  public static void main(String[] args) throws Exception {
    final String env = args[0];
    final String releaseName = args[1];
    final String projectKey = args[2];
    final String config = CONFIG.valueOf(env).filename;
    checkArgument(env != null);
    checkArgument(releaseName != null);
    checkArgument(projectKey != null);
    checkArgument(config != null);
    log.info("env = {} ", env);
    log.info("releaseName = {} ", releaseName);
    log.info("projectKey = {} ", projectKey);
    log.info("config = {} ", config);

    System.setProperty(HADOOP_USER_NAME_PARAM, HADOOP_USER_NAME); // see DCC-572

    Injector injector = Guice.createInjector(new ConfigModule(ConfigFactory.load(config))//
        , new MorphiaModule()//
        , new FileSystemModule()//
        , new ValidationModule()//
        );

    ReleaseService releaseService = injector.getInstance(ReleaseService.class);
    Release release = getRelease(releaseService, releaseName);
    if(null != release) {
      ValidationService validationService = injector.getInstance(ValidationService.class);
      ValidationQueueManagerService validationQueueManagerService =
          injector.getInstance(ValidationQueueManagerService.class);
      Plan plan = validationService.prepareValidation(release, new QueuedProject(projectKey, null),
          validationQueueManagerService.new ValidationCascadeListener());
      validationService.startValidation(plan);
    } else {
      log.info("there is no next release at the moment");
    }
  }

  private static Release getRelease(ReleaseService releaseService, final String releaseName) {
    NextRelease nextRelease = releaseService.getNextRelease();
    if(null != nextRelease) {
      Release release = nextRelease.getRelease();
      if(releaseName.equals(release.getName())) {
        return release;
      } else {
        Iterable<CompletedRelease> filter =
            Iterables.filter(releaseService.getCompletedReleases(), new Predicate<CompletedRelease>() {
              @Override
              public boolean apply(CompletedRelease input) {
                return releaseName.equals(input.getRelease().getName());
              }
            });
        return filter.iterator().hasNext() ? filter.iterator().next().getRelease() : null;
      }
    } else {
      return null;
    }
  }
}
