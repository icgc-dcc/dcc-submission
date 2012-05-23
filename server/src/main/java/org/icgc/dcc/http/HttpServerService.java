package org.icgc.dcc.http;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.util.Set;

import javax.inject.Inject;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractService;
import com.typesafe.config.Config;

/**
 * A {@code Service} for managing the {@code HttpServer} lifecycle.
 */
public class HttpServerService extends AbstractService {

  private static final Logger log = LoggerFactory.getLogger(HttpServerService.class);

  private final Config config;

  private final HttpServer server;

  private final Set<HttpHandlerProvider> handlerProviders;

  @Inject
  public HttpServerService(Config config, HttpServer server, Set<HttpHandlerProvider> handlerProviders) {
    checkArgument(config != null);
    checkArgument(server != null);
    checkArgument(handlerProviders != null);
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
    for(HttpHandlerProvider provider : handlerProviders) {
      serverConfig.addHttpHandler(provider.get(), provider.path());
    }

    // TODO: add a Handler for static files. This is tied to the way we package and deploy the app.
    // serverConfig.addHttpHandler(new
    // StaticHttpHandler(ImmutableSet.copyOf(config.getStringList("http.resources"))),"/");

    try {
      server.start();
      notifyStarted();
    } catch(IOException ex) {
      log.error("Failed to start HTTP server on {}:{} : {}", new Object[] { host, port, ex.getMessage() });
      notifyFailed(ex);
    }
  }

  @Override
  protected void doStop() {
    server.stop();
    notifyStopped();
  }
}
