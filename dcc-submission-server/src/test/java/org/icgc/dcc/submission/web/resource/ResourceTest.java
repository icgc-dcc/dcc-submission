package org.icgc.dcc.submission.web.resource;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.glassfish.jersey.internal.util.Base64.encodeAsString;
import static org.icgc.dcc.submission.test.Tests.TEST_CONFIG_FILE;

import java.io.IOException;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.Application;

import org.apache.shiro.util.ThreadContext;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.icgc.dcc.submission.config.PersistenceConfig;
import org.icgc.dcc.submission.config.ServerConfig;
import org.icgc.dcc.submission.config.ValidationConfig;
import org.icgc.dcc.submission.fs.FileSystemConfig;
import org.icgc.dcc.submission.http.jersey.JerseyConfig;
import org.icgc.dcc.submission.repository.RepositoryConfig;
import org.icgc.dcc.submission.service.ServiceConfig;
import org.icgc.dcc.submission.sftp.SftpConfig;
import org.icgc.dcc.submission.shiro.ShiroConfig;
import org.icgc.dcc.submission.test.TestConfig;
import org.icgc.dcc.submission.web.WebConfig;
import org.junit.After;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.net.HttpHeaders;

import lombok.val;

public abstract class ResourceTest extends JerseyTest {

  private static final String AUTH_HEADER = HttpHeaders.AUTHORIZATION.toString();
  private static final String AUTH_VALUE = "X-DCC-Auth " + encodeAsString("admin:adminspasswd");
  protected static final String MIME_TYPE = APPLICATION_JSON;

  private ConfigurableApplicationContext context;

  @After
  public void after() {
    // Clean-up threads
    ThreadContext.remove();

    context.close();
  }

  @Override
  public TestContainerFactory getTestContainerFactory() {
    return new InMemoryTestContainerFactory();
  }

  @Override
  protected Application configure() {
    val builder = new SpringApplicationBuilder(
        TestConfig.class,
        JerseyConfig.class,
        ShiroConfig.class,
        WebConfig.class,
        PersistenceConfig.class,
        FileSystemConfig.class,
        RepositoryConfig.class,
        ServiceConfig.class,
        SftpConfig.class,
        ValidationConfig.class,
        ServerConfig.class);

    register(builder);
    val app = builder.build();
    context = app.run("--spring.config.location=" + TEST_CONFIG_FILE.getAbsolutePath());

    context.getBeanFactory().autowireBean(this);

    return context.getBean(ResourceConfig.class);
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

  protected void register(SpringApplicationBuilder builder) {
  }

  protected <T> T getBean(Class<T> clazz) {
    return context.getBean(clazz);
  }

}
