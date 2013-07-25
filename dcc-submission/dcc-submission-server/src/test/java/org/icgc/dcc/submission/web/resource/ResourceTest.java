package org.icgc.dcc.submission.web.resource;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.inject.util.Modules.EMPTY_MODULE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.glassfish.grizzly.http.util.Header.Authorization;
import static org.glassfish.jersey.internal.util.Base64.encodeAsString;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Application;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.icgc.dcc.submission.config.ConfigModule;
import org.icgc.dcc.submission.core.CoreModule;
import org.icgc.dcc.submission.core.morphia.MorphiaModule;
import org.icgc.dcc.submission.fs.FileSystemModule;
import org.icgc.dcc.submission.http.jersey.JerseyModule;
import org.icgc.dcc.submission.sftp.SftpModule;
import org.icgc.dcc.submission.shiro.ShiroModule;
import org.icgc.dcc.submission.validation.ValidationModule;
import org.icgc.dcc.submission.web.WebModule;

import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.typesafe.config.ConfigFactory;

public abstract class ResourceTest extends JerseyTest {

  private static final String AUTH_HEADER = Authorization.toString();

  private static final String AUTH_VALUE = "X-DCC-Auth " + encodeAsString("admin:adminspasswd");

  protected static final String MIME_TYPE = APPLICATION_JSON;

  protected Injector injector;

  @Override
  public TestContainerFactory getTestContainerFactory() {
    return new InMemoryTestContainerFactory();
  }

  @Override
  protected Application configure() {
    List<Module> modules = newArrayList(//
        // Infrastructure modules
        (Module) new ConfigModule(ConfigFactory.load()), //
        (Module) new CoreModule(), //
        (Module) new JerseyModule(), //
        (Module) new WebModule(), //
        (Module) new MorphiaModule(), //
        (Module) new ShiroModule(), //
        (Module) new FileSystemModule(), //
        (Module) new SftpModule(), //

        // Business modules
        (Module) new ValidationModule());

    modules.addAll(configureModules());

    injector = Guice.createInjector(modules);

    return injector.getInstance(ResourceConfig.class);
  }

  @Override
  protected void configureClient(ClientConfig clientConfig) {
    clientConfig.register(JacksonJsonProvider.class);
    clientConfig.register(new ClientRequestFilter() {

      @Override
      public void filter(ClientRequestContext requestContext) throws IOException {
        requestContext.getHeaders().add(AUTH_HEADER, AUTH_VALUE);
      }
    });
  }

  /**
   * To be overriden if more modules are necessary (and to mock them for instance).
   */
  protected Collection<? extends Module> configureModules() {
    return ImmutableList.of(EMPTY_MODULE);
  }

}
