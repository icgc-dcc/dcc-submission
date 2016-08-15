/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
 *                                                                                                               
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with                                  
 * this program. If not, see <http://www.gnu.org/licenses/>.                                                     
 *                                                                                                               
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY                           
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES                          
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT                           
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,                                
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED                          
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;                               
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER                              
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN                         
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.submission.http;

import static com.google.common.base.Throwables.propagate;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS;
import static com.google.common.net.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.icgc.dcc.submission.core.config.SubmissionProperties;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.util.concurrent.AbstractService;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

/**
 * A {@code Service} for managing the {@code HttpServer} life cycle.
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class HttpServerService extends AbstractService {

  /**
   * Constants.
   */
  private static final String PROTOCOL = "TLS";
  private static final String LISTENER_NAME = "dcc_https_listener";
  private static final String CERTIFICATE = "/keystore";
  private static final String CERTIFICATE_PASSWORD = "tmptmptmp";

  /**
   * Server state.
   */
  @NonNull
  private final SubmissionProperties properties;
  @NonNull
  private final HttpServer server;
  @NonNull
  private final Set<HttpHandlerProvider> handlerProviders;

  @Override
  protected void doStart() {
    try {
      val host = properties.getHttp().getListen();
      val port = properties.getHttp().getPort();
      val useSsl = properties.getHttp().getSsl();
      val resources = properties.getHttp().getResources();
      log.info("HTTP config: host = {}, port = {}, use SSL = {}, resources = {}",
          new Object[] { host, port, useSsl, resources });

      addListeners(host, port, useSsl);
      addHandlers(resources);

      try {
        server.start();
        notifyStarted();
      } catch (IOException e) {
        log.error("Failed to start HTTP server on {}:{} : {}", new Object[] { host, port, e.getMessage() });
        notifyFailed(e);
      }
    } catch (Exception e) {
      log.error("Failed to start HTTP server", e);
    }
  }

  @Override
  protected void doStop() {
    server.stop();
    notifyStopped();
  }

  private void addHandlers(Set<String> resources) {
    val serverConfig = server.getServerConfiguration();

    for (val provider : handlerProviders) {
      serverConfig.addHttpHandler(provider.get(), provider.path());
    }

    serverConfig.addHttpHandler(new StaticCorsHttpHandler(resources), "/");
    serverConfig.addHttpHandler(new RedirectHttpHandler(), "/releases", "/login");
  }

  private void addListeners(String host, final int port, final boolean useSsl) {
    val networkListener = new NetworkListener(LISTENER_NAME, host, port);
    if (useSsl) {
      networkListener.setSecure(true);
      networkListener.setSSLEngineConfig(createSSLEngineConfigurator(CERTIFICATE_PASSWORD.toCharArray()));
    }

    server.addListener(networkListener);
  }

  /**
   * Creates {@code SSLEngineConfigurator} object necessary to configure self-signed certificate for SSL.
   */
  private SSLEngineConfigurator createSSLEngineConfigurator(char[] password) {
    val keyStore = createKeyStore(password);
    val keyManagerFactory = createKeyManagerFactory(password, keyStore);
    val trustManagerFactory = createTrustManagerFactory(keyStore);
    val sslContext = createSslContext(keyManagerFactory, trustManagerFactory);

    return new SSLEngineConfigurator(sslContext, false, false, false);
  }

  private KeyStore createKeyStore(char[] password) {
    KeyStore keyStore = null;
    try {
      log.info("Using certificate: {}", CERTIFICATE);
      keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

      @Cleanup
      InputStream inputStream = this.getClass().getResourceAsStream(CERTIFICATE);
      if (inputStream == null) {
        log.error("Cannot find certificate: {}", CERTIFICATE);
        throw new CertificateNotFoundException(CERTIFICATE);
      }

      keyStore.load(inputStream, password);
    } catch (KeyStoreException e) {
      log.error("Failed to create key store", e);
      propagate(e);
    } catch (NoSuchAlgorithmException e) {
      log.error("Failed to create key store", e);
      propagate(e);
    } catch (CertificateException e) {
      log.error("Failed to create key store", e);
      propagate(e);
    } catch (IOException e) {
      log.error("Failed to create key store", e);
      propagate(e);
    }

    return keyStore;
  }

  private SSLContext createSslContext(KeyManagerFactory keyManagerFactory, TrustManagerFactory trustManagerFactory) {
    SSLContext sslContext = null;
    try {
      sslContext = SSLContext.getInstance(PROTOCOL);
      sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), null);
    } catch (NoSuchAlgorithmException e) {
      log.error("Failed to create SSL context", e);
      propagate(e);
    } catch (KeyManagementException e) {
      log.error("Failed to initialize SSL context", e);
      propagate(e);
    }

    return sslContext;
  }

  private TrustManagerFactory createTrustManagerFactory(KeyStore keyStore) {
    TrustManagerFactory trustManagerFactory = null;
    try {
      trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    } catch (NoSuchAlgorithmException e) {
      log.error("Failed to create trust manager factory", e);
      propagate(e);
    }

    try {
      trustManagerFactory.init(keyStore);
    } catch (KeyStoreException e) {
      log.error("Failed to initialize trust manager factory", e);
      propagate(e);
    }

    return trustManagerFactory;
  }

  private KeyManagerFactory createKeyManagerFactory(char[] password, KeyStore keyStore) {
    KeyManagerFactory keyManagerFactory = null;
    try {
      keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    } catch (NoSuchAlgorithmException e) {
      log.error("Failed to create key manager factory", e);
      propagate(e);
    }
    try {
      keyManagerFactory.init(keyStore, password);
    } catch (UnrecoverableKeyException e) {
      log.error("Failed to initialize key manager factory", e);
      propagate(e);
    } catch (KeyStoreException e) {
      log.error("Failed to initialize key manager factory", e);
      propagate(e);
    } catch (NoSuchAlgorithmException e) {
      log.error("Failed to initialize key manager factory", e);
      propagate(e);
    }

    return keyManagerFactory;
  }

  private static class RedirectHttpHandler extends HttpHandler {

    @Override
    public void service(Request request, Response response) throws Exception {
      // Redirect back to "/" and appends the request url after the hash(#), which the client can then parse
      response.sendRedirect("/#" + request.getDecodedRequestURI());
    }
  }

  private static class StaticCorsHttpHandler extends StaticHttpHandler {

    private StaticCorsHttpHandler(Set<String> docRoots) {
      super(docRoots);
      setFileCacheEnabled(false);
    }

    @Override
    protected boolean handle(String uri, Request request, Response response) throws Exception {
      // Add CORS headers for cross domain access to static resources
      response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, "*");
      response.addHeader(ACCESS_CONTROL_ALLOW_HEADERS, "origin, content-type, accept, authorization");
      response.addHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
      response.addHeader(ACCESS_CONTROL_ALLOW_METHODS, "GET, OPTIONS, HEAD");

      return super.handle(uri, request, response);
    }
  }

}
