package org.icgc.dcc.web;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.inject.util.Modules.EMPTY_MODULE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.glassfish.grizzly.http.util.Header.Authorization;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Application;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.internal.util.Base64;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.icgc.dcc.config.ConfigModule;
import org.icgc.dcc.core.CoreModule;
import org.icgc.dcc.core.morphia.MorphiaModule;
import org.icgc.dcc.dictionary.DictionaryModule;
import org.icgc.dcc.filesystem.FileSystemModule;
import org.icgc.dcc.http.jersey.JerseyModule;
import org.icgc.dcc.release.ReleaseModule;
import org.icgc.dcc.sftp.SftpModule;
import org.icgc.dcc.shiro.ShiroModule;
import org.icgc.dcc.validation.ValidationModule;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.typesafe.config.ConfigFactory;

public abstract class ResourceTest extends JerseyTest {

  private static final String AUTH_HEADER = Authorization.toString();

  private static final String AUTH_VALUE = "X-DCC-Auth " + Base64.encodeAsString("admin:adminspasswd");

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
        (Module) new DictionaryModule(), //
        (Module) new ReleaseModule(), //
        (Module) new ValidationModule());

    modules.add(configureModule());

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

  protected Module configureModule() {
    return EMPTY_MODULE;
  }

}
