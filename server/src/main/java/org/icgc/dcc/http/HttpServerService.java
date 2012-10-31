package org.icgc.dcc.http;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
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

  private static final String LISTENER_NAME = "dcc_https_listener";

  private static final String PROTOCOL = "TLS";

  private static final String CERTIFICATE = "/keystore";

  private static final String CERTIFICATE_PASSWORD = "tmptmptmp"; // FIXME

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
    final boolean useSsl = true;// config.getBoolean("http.ssl");
    final Set<String> resources = ImmutableSet.copyOf(config.getStringList("http.resources"));
    log.info("host = {}, port = {}, use SSL = {}, resources = {}", new Object[] { host, port, useSsl, resources });

    // add network listener
    NetworkListener networkListener = new NetworkListener(LISTENER_NAME, host, port);
    if(useSsl) {
      networkListener.setSecure(true);
      networkListener.setSSLEngineConfig(createSSLEngineConfigurator());
    }
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

  /**
   * Creates {@code SSLEngineConfigurator} object necessary to configure self-signed certificate for SSL.
   */
  private SSLEngineConfigurator createSSLEngineConfigurator() {
    SSLEngineConfigurator sslEngineConfigurator = null;
    try {
      char[] password = CERTIFICATE_PASSWORD.toCharArray();
      KeyStore keyStore = createKeyStore(password);

      KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(keyStore, password);

      TrustManagerFactory trustManagerFactory =
          TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      trustManagerFactory.init(keyStore);

      SSLContext sslContext = SSLContext.getInstance(PROTOCOL);
      sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);

      sslEngineConfigurator = new SSLEngineConfigurator(sslContext, false, false, false);
    } catch(UnrecoverableKeyException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch(KeyStoreException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch(FileNotFoundException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch(CertificateException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch(IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch(NoSuchAlgorithmException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch(KeyManagementException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return sslEngineConfigurator;
  }

  /**
   * Creates a {@code KeyStore} to point to our self-signed certificate.
   */
  private KeyStore createKeyStore(char[] password) throws KeyStoreException, IOException, NoSuchAlgorithmException,
      CertificateException {
    log.info("using certificate: {}", CERTIFICATE);
    KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
    InputStream is = this.getClass().getResourceAsStream(CERTIFICATE);
    if(is == null) {
      log.error("cannot find: {}", CERTIFICATE);
      throw new RuntimeException(CERTIFICATE);// TODO
    }
    keyStore.load(is, password);
    is.close();
    return keyStore;
  }

  @Override
  protected void doStop() {
    server.stop();
    notifyStopped();
  }
}
