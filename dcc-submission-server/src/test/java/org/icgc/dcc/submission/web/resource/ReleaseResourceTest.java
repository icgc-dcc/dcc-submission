package org.icgc.dcc.submission.web.resource;

import static com.google.common.collect.Lists.newArrayList;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.service.ReleaseService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;

import lombok.val;

public class ReleaseResourceTest extends ResourceTest {

  @Autowired
  Release release;

  @Configuration
  static class ResourceConfig {

    @Bean
    public Release release() {
      val release = new Release();
      release.setDictionaryVersion("0.6e");
      release.setName("ICGC13");
      release.setReleaseDate();
      release.addSubmission(new Submission("project1", "project one", release.getName()));
      release.addSubmission(new Submission("project2", "project two", release.getName()));

      return release;
    }

    @Bean
    public ReleaseService releaseService() {
      val mockReleaseService = mock(ReleaseService.class);
      when(mockReleaseService.getReleasesBySubject(any(Authentication.class))).thenReturn(newArrayList(release()));

      return mockReleaseService;
    }

  }

  @Override
  protected void register(SpringApplicationBuilder builder) {
    builder.sources(ResourceConfig.class);
  }

  @Test
  public void testGetReleases() {
    val reponse = (OutboundJaxrsResponse) target().path("releases").request(MIME_TYPE).get();

    assertThat(reponse.getStatus()).isEqualTo(OK.getStatusCode());
    assertThat(reponse.readEntity(String.class))
        .isEqualTo(
            "[{\"name\":\"ICGC13\",\"state\":\"OPENED\",\"releaseDate\":"
                + release.getReleaseDate().getTime()
                + ",\"dictionaryVersion\":\"0.6e\",\"submissions\":[{\"projectKey\":\"project1\",\"projectName\":\"project one\",\"releaseName\":\"ICGC13\"},"
                + "{\"projectKey\":\"project2\",\"projectName\":\"project two\",\"releaseName\":\"ICGC13\"}]}]");
  }

}
