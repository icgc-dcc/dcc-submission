package org.icgc.dcc.web;

import static org.fest.assertions.api.Assertions.assertThat;

import org.icgc.dcc.core.model.Status;
import org.icgc.dcc.sftp.SftpServerService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SystemResourceTest extends ResourceTest {

  SftpServerService service;

  @Override
  @Before
  public void setUp() {
    service = injector.getInstance(SftpServerService.class);
    service.startAndWait();
  }

  @Test
  public void testStatus() {
    Status status = target().path("system").path("status").request(MIME_TYPE).get(Status.class);

    assertThat(status.getActiveSftpSessions()).isEqualTo(0);
  }

  @Override
  @After
  public void tearDown() {
    service.stopAndWait();
  }

}
