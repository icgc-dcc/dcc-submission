package org.icgc.dcc.http;

import com.google.common.util.concurrent.AbstractService;
import com.typesafe.config.Config;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.jersey.internal.ProcessingException;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Set;

public class DccHttpServer extends AbstractService {

  private final Config config;

  private final HttpServer server;

  private final Set<HttpHandlerProvider> handlerProviders;

  @Inject
  public DccHttpServer(Config config, HttpServer server, Set<HttpHandlerProvider> handlerProviders) {
    this.config = config;
    this.server = server;
    this.handlerProviders = handlerProviders;
  }

  @Override
  protected void doStart() {
    final String host = config.getString("http.listen");
    final int port = config.getInt("http.port");
    server.addListener(new NetworkListener("http", host, port));

    final ServerConfiguration serverConfig = server.getServerConfiguration();
    for (HttpHandlerProvider provider : handlerProviders) {
      serverConfig.addHttpHandler(provider.get(), provider.path());
    }

    //serverConfig.addHttpHandler(new StaticHttpHandler(ImmutableSet.copyOf(config.getStringList("http.resources"))), "/");

    try {
      server.start();
      notifyStarted();
    } catch (IOException ex) {
      throw new ProcessingException("IOException thrown when trying to start grizzly server", ex);
    }
  }

  @Override
  protected void doStop() {
    server.stop();
    notifyStopped();
  }
}
