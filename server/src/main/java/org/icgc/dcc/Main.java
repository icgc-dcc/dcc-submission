package org.icgc.dcc;

import java.io.IOException;

import org.icgc.dcc.config.ConfigModule;
import org.icgc.dcc.core.CoreModule;
import org.icgc.dcc.core.DccRuntime;
import org.icgc.dcc.filesystem.DccFilesystem;
import org.icgc.dcc.filesystem.FilesystemModule;
import org.icgc.dcc.http.HttpModule;
import org.icgc.dcc.http.jersey.InjectModule;
import org.icgc.dcc.http.jersey.JerseyModule;
import org.icgc.dcc.model.ModelModule;
import org.icgc.dcc.shiro.MyShiro;
import org.icgc.dcc.shiro.ShiroModule;
import org.icgc.dcc.web.WebModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.typesafe.config.ConfigFactory;

public class Main {
  public static void main(String[] args) throws IOException {
    Injector injector = Guice.createInjector(new ConfigModule(ConfigFactory.load()),//
        new CoreModule(),//
        new HttpModule(),//
        new InjectModule(),//
        new JerseyModule(),//
        new WebModule(),//
        new ModelModule(),//
        new ShiroModule(),//
        new FilesystemModule()//
        );

    // for development purposes only (TODO: remove)
    injector.getInstance(MyShiro.class).doIt();
    try {
      if("anthony".equals(System.getProperty("user.name"))) {// FIXME: temporary workaround, so program does not break
                                                             // for those who don't have hadoop/test environment setup
        injector.getInstance(DccFilesystem.class).doIt();
      }
    } catch(Exception e) { // TODO: what's our policy on exception for now?
      throw new RuntimeException(e);
    }

    injector.getInstance(DccRuntime.class).start();
    System.in.read();
    injector.getInstance(DccRuntime.class).stop();

  }
}
