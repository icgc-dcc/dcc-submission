package org.icgc.dcc;

import java.io.IOException;

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

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.typesafe.config.ConfigFactory;

public class Main {

  private static final String HADOOP_USER_NAME_PARAM = "HADOOP_USER_NAME";

  private static final String HADOOP_USER_NAME = "hdfs";

  private static enum CONFIG {
    qa("application_qa"), dev("application_dev"), local("application");

    String filename;

    private CONFIG(String filename) {
      this.filename = filename;
    }
  };

  public static void main(String[] args) throws IOException {
    String config = (args != null && args.length > 0) ? CONFIG.valueOf(args[0]).filename : "application";
    System.setProperty(HADOOP_USER_NAME_PARAM, HADOOP_USER_NAME); // see DCC-572

    Injector injector = Guice.createInjector(new ConfigModule(ConfigFactory.load(config))//
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
        , new ValidationModule()//
        );

    injector.getInstance(DccRuntime.class).start();
    System.in.read();
    injector.getInstance(DccRuntime.class).stop();

  }
}
