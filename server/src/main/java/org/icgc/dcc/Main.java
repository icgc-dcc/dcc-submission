package org.icgc.dcc;

import java.io.IOException;

import org.icgc.dcc.config.ConfigModule;
import org.icgc.dcc.http.HttpModule;
import org.icgc.dcc.http.HttpServerService;
import org.icgc.dcc.http.jersey.InjectModule;
import org.icgc.dcc.http.jersey.JerseyModule;
import org.icgc.dcc.model.ModelModule;
import org.icgc.dcc.web.WebModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.typesafe.config.ConfigFactory;

public class Main {
  public static void main(String[] args) throws IOException {
    Injector injector = Guice.createInjector(new ConfigModule(ConfigFactory.load()),//
        new HttpModule(),//
        new InjectModule(),//
        new JerseyModule(),//
        new WebModule(),//
        new ModelModule());
    injector.getInstance(HttpServerService.class).startAndWait();
    System.in.read();
    injector.getInstance(HttpServerService.class).stop();
  }
}
