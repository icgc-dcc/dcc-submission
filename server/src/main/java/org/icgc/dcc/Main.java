package org.icgc.dcc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;

import org.icgc.dcc.config.ConfigModule;
import org.icgc.dcc.core.CoreModule;
import org.icgc.dcc.core.DccRuntime;
import org.icgc.dcc.core.morphia.MorphiaModule;
import org.icgc.dcc.dictionary.DictionaryModule;
import org.icgc.dcc.filesystem.FileSystemModule;
import org.icgc.dcc.http.HttpModule;
import org.icgc.dcc.http.jersey.JerseyModule;
import org.icgc.dcc.release.ReleaseModule;
import org.icgc.dcc.sftp.SftpModule;
import org.icgc.dcc.shiro.ShiroModule;
import org.icgc.dcc.validation.ValidationModule;
import org.icgc.dcc.web.WebModule;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Main {

  private static final String HADOOP_USER_NAME_PARAM = "HADOOP_USER_NAME";

  private static final String HADOOP_USER_NAME = "hdfs";

  private static enum CONFIG {
    qa("application_qa"), dev("application_dev"), local("application"), external(null);

    String filename;

    private CONFIG(String filename) {
      this.filename = filename;
    }

    public static String listValues() {
      return Arrays.asList(CONFIG.values()).toString();
    }
  };

  private static Injector injector;

  public static void main(String[] args) throws IOException {

    Config parsedConfig = loadConfig(args);

    System.setProperty(HADOOP_USER_NAME_PARAM, HADOOP_USER_NAME); // see DCC-572
    Main.injector = Guice.createInjector(new ConfigModule(parsedConfig) //
        , new CoreModule()//
        , new HttpModule()//
        , new JerseyModule()//
        , new WebModule()//
        , new MorphiaModule()//
        , new ShiroModule()//
        , new FileSystemModule()//
        , new SftpModule()//
        , new DictionaryModule()//
        , new ReleaseModule()//
        , new ValidationModule());

    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      @Override
      public void run() {
        // No one call shutdown?
        boolean running = injector != null;
        if(running) {
          injector.getInstance(DccRuntime.class).stop();
        }
      }

    }, "Shutdown-thread"));

    injector.getInstance(DccRuntime.class).start();
  }

  private static Config loadConfig(String[] args) throws FileNotFoundException {
    CONFIG configType;
    try {
      configType = (args != null && args.length > 0) ? CONFIG.valueOf(args[0]) : CONFIG.local;
    } catch(IllegalArgumentException e) {
      throw new IllegalArgumentException(args[0] + " is not a valid argument. Valid arguments are "
          + CONFIG.listValues());
    }
    Config parsedConfig;
    if(configType == CONFIG.external) {
      if(args.length < 2) {
        throw new IllegalArgumentException("The argument 'external' requires a filename as an additional parameter");
      }
      File configFile = new File(args[1]);
      if(configFile.exists() == false) {
        throw new FileNotFoundException(args[1]);
      }
      parsedConfig = ConfigFactory.parseFile(configFile).resolve();
    } else {
      parsedConfig = ConfigFactory.load(configType.filename);
    }
    return parsedConfig;
  }

  /**
   * This method is required for testing since shutdown hooks are not invoked between tests.
   */
  @VisibleForTesting
  public static void shutdown() {
    injector.getInstance(DccRuntime.class).stop();
    injector = null;
  }

}
