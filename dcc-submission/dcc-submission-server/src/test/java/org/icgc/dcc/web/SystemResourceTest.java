package org.icgc.dcc.web;

import static com.google.common.util.concurrent.Service.State.RUNNING;
import static com.google.common.util.concurrent.Service.State.TERMINATED;
import static javax.ws.rs.client.Entity.json;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.MOVED_PERMANENTLY;
import static org.fest.assertions.api.Assertions.assertThat;

import java.net.URISyntaxException;

import javax.ws.rs.core.Response;

import org.icgc.dcc.core.model.Status;
import org.icgc.dcc.sftp.SftpServerService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SystemResourceTest extends ResourceTest {

  private static final String PATCH = "PATCH";

  SftpServerService service;

  @Override
  @Before
  public void setUp() {
    service = injector.getInstance(SftpServerService.class);
    service.startAndWait();
  }

  @Test
  public void testRedirectStatus() throws URISyntaxException {
    Response response = target().path("system").path("status").request(MIME_TYPE).get(Response.class);
    assertThat(response.getStatus()).isEqualTo(MOVED_PERMANENTLY.getStatusCode());
    assertThat(response.getLocation().getPath()).isEqualTo("/systems/sftp");
  }

  @Test
  public void testGetStatus() {
    Status status = target().path("systems").path("sftp").request(MIME_TYPE).get(Status.class);
    assertThat(status.getActiveSftpSessions()).isEqualTo(0);
    assertThat(status.getSftpState()).isEqualTo(RUNNING);
  }

  @Test
  public void testDisableSftp() {
    Status status =
        target().path("systems").path("sftp").request(MIME_TYPE)
            .method(PATCH, json("{\"active\":false}"), Status.class);
    assertThat(status.getActiveSftpSessions()).isEqualTo(0);
    assertThat(status.getSftpState()).isEqualTo(TERMINATED);
  }

  @Test
  public void testEnableSftp() {
    Status status =
        target().path("systems").path("sftp").request(MIME_TYPE)
            .method(PATCH, json("{\"active\":true}"), Status.class);
    assertThat(status.getActiveSftpSessions()).isEqualTo(0);
    assertThat(status.getSftpState()).isEqualTo(RUNNING);
  }

  @Test
  public void testInvalidSftpPatch() {
    Response response =
        target().path("systems").path("sftp").request(MIME_TYPE).method(PATCH, json("{}"), Response.class);
    assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
  }

  @Override
  @After
  public void tearDown() {
    service.stopAndWait();
  }

}
