package org.icgc.dcc;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.typesafe.config.ConfigFactory;
import org.icgc.dcc.config.ConfigModule;
import org.icgc.dcc.http.DccHttpServer;
import org.icgc.dcc.http.HttpModule;
import org.icgc.dcc.http.jersey.JerseyModule;
import org.icgc.dcc.web.WebModule;
import org.icgc.dcc.web.inject.InjectModule;

import java.io.IOException;

public class Main {
  public static void main(String[] args) throws IOException {
    Injector injector = Guice.createInjector(new ConfigModule(ConfigFactory.load()), new HttpModule(), new InjectModule(), new JerseyModule(), new WebModule());
    injector.getInstance(DccHttpServer.class).startAndWait();
    System.in.read();
    injector.getInstance(DccHttpServer.class).stop();
  }
}

