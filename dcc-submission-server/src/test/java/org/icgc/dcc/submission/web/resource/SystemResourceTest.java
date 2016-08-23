package org.icgc.dcc.submission.web.resource;

import static com.google.common.util.concurrent.Service.State.RUNNING;
import static javax.ws.rs.client.Entity.json;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;

import javax.ws.rs.core.Response;

import org.icgc.dcc.submission.core.model.Status;
import org.icgc.dcc.submission.sftp.SftpServerService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SystemResourceTest extends ResourceTest {

  private static final String PATCH = "PATCH";

  SftpServerService service;

  @Override
  @Before
  public void setUp() {
    service = getBean(SftpServerService.class);
    service.awaitRunning();
  }

  @Test
  public void testGetStatus() {
    Status status = target().path("systems").request(MIME_TYPE).get(Status.class);
    assertThat(status.getActiveSftpSessions()).isEqualTo(0);
    assertThat(status.getSftpState()).isEqualTo(RUNNING);
    assertThat(status.isSftpEnabled()).isTrue();
  }

  @Test
  public void testDisableSftp() {
    Status status =
        target().path("systems").request(MIME_TYPE)
            .method(PATCH, json("{\"active\":false}"), Status.class);
    assertThat(status.getActiveSftpSessions()).isEqualTo(0);
    assertThat(status.getSftpState()).isEqualTo(RUNNING);
    assertThat(status.isSftpEnabled()).isFalse();
  }

  @Test
  public void testEnableSftp() {
    Status status =
        target().path("systems").request(MIME_TYPE)
            .method(PATCH, json("{\"active\":true}"), Status.class);
    assertThat(status.getActiveSftpSessions()).isEqualTo(0);
    assertThat(status.getSftpState()).isEqualTo(RUNNING);
    assertThat(status.isSftpEnabled()).isTrue();
  }

  @Test
  public void testInvalidSftpPatch() {
    Response response =
        target().path("systems").request(MIME_TYPE).method(PATCH, json("{}"), Response.class);
    assertThat(response.getStatus()).isEqualTo(BAD_REQUEST.getStatusCode());
  }

  @Override
  @After
  public void tearDown() {
    service.stopAsync().awaitTerminated();
  }

}
