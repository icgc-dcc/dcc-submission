package org.icgc.dcc.http;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

import javax.inject.Inject;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
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
    final Set<String> resources = ImmutableSet.copyOf(config.getStringList("http.resources"));

    // add network listener
    NetworkListener networkListener = new NetworkListener("dcc_https_listener", host, port); // http://grizzly.java.net/nonav/docs/2.0/apidocs/org/glassfish/grizzly/http/server/NetworkListener.html

    SSLEngineConfigurator sslEngineConfigurator = null;
    try {
      sslEngineConfigurator = createSSLEngineConfigurator();
    } catch(NoSuchAlgorithmException e) {
      e.printStackTrace(); // TODO
    } catch(KeyManagementException e) {
      e.printStackTrace(); // TODO
    }

    networkListener.setSecure(true);
    networkListener.setSSLEngineConfig(sslEngineConfigurator);

    server.addListener(networkListener);

    // add the http handlers
    final ServerConfiguration serverConfig = server.getServerConfiguration();
    for(HttpHandlerProvider provider : handlerProviders) {
      serverConfig.addHttpHandler(provider.get(), provider.path());
    }
    serverConfig.addHttpHandler(new StaticHttpHandler(resources), "/");

    // Redirect back to "/" and appends the request url after the hash(#), which the client can then parse
    serverConfig.addHttpHandler(new HttpHandler() {
      @Override
      public void service(Request request, Response response) throws Exception {
        response.sendRedirect("/#" + request.getDecodedRequestURI());
      }
    }, "/releases");

    try {
      server.start();
      notifyStarted();
    } catch(IOException ex) {
      log.error("Failed to start HTTP server on {}:{} : {}", new Object[] { host, port, ex.getMessage() });
      notifyFailed(ex);
    }
  }

  private SSLEngineConfigurator createSSLEngineConfigurator() throws NoSuchAlgorithmException, KeyManagementException {
    KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(//
        KeyManagerFactory.getDefaultAlgorithm()); // Prepare a key manager using the provided keystore

    TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(//
        TrustManagerFactory.getDefaultAlgorithm());

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

    return new SSLEngineConfigurator(sslContext, false, false, false);
  }

  @Override
  protected void doStop() {
    server.stop();
    notifyStopped();
  }
}
