package org.icgc.dcc.submission.web.resource;

import static javax.ws.rs.core.Response.Status.OK;
import static org.elasticsearch.common.collect.Lists.newArrayList;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;

import javax.ws.rs.core.Application;

import lombok.val;

import org.apache.shiro.subject.Subject;
import org.icgc.dcc.submission.core.AbstractDccModule;
import org.icgc.dcc.submission.release.ReleaseService;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;

public class ReleaseResourceTest extends ResourceTest {

  private Release release;
  private ReleaseService mockReleaseService;

  @Override
  protected Application configure() {
    release = new Release();
    release.setDictionaryVersion("0.6d");
    release.setName("ICGC13");
    release.setReleaseDate();
    release.setTransitioning(true);
    release.addSubmission(new Submission("project1"));
    release.addSubmission(new Submission("project2"));

    mockReleaseService = mock(ReleaseService.class);
    when(mockReleaseService.getReleasesBySubject(any(Subject.class))).thenReturn(newArrayList(release));
    return super.configure();
  }

  @Test
  public void testGetReleases() {
    val reponse = target().path("releases").request(MIME_TYPE).get();

    assertThat(reponse.getStatus()).isEqualTo(OK.getStatusCode());
    assertThat(reponse.readEntity(String.class))
        .isEqualTo(
            "[{\"name\":\"ICGC13\",\"state\":\"OPENED\",\"releaseDate\":" + release.getReleaseDate().getTime()
                + ",\"dictionaryVersion\":\"0.6d\"}]");
  }

  @Override
  protected Collection<? extends Module> configureModules() {
    return ImmutableList.of(new AbstractDccModule() {

      @Override
      protected void configure() {
        bind(ReleaseService.class).toInstance(mockReleaseService);
      }
    });
  }

}
