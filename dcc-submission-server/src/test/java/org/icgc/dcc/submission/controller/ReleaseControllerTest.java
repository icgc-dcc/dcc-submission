package org.icgc.dcc.submission.controller;

import static com.google.common.collect.Lists.newArrayList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.service.ReleaseService;
import org.icgc.dcc.submission.service.SystemService;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;

@WebMvcTest(ReleaseController.class)
public class ReleaseControllerTest extends ControllerTest {

  Release release;

  @MockBean
  private ReleaseService releaseService;
  @MockBean
  private SystemService systemService;

  @Before
  public void setUp() {
    release = new Release();
    release.setDictionaryVersion("0.6e");
    release.setName("ICGC13");
    release.setReleaseDate();
    release.addSubmission(new Submission("project1", "project one", release.getName()));
    release.addSubmission(new Submission("project2", "project two", release.getName()));

    when(releaseService.getReleasesBySubject(any(Authentication.class))).thenReturn(newArrayList(release));
  }

  @Test
  public void testGetReleases() throws Exception {
    mvc
        .perform(
            get("/ws/releases")
                .accept(MediaType.APPLICATION_JSON)
                .with(admin()))
        .andExpect(status().isOk())
        .andExpect(content().string("[{\"name\":\"ICGC13\",\"state\":\"OPENED\",\"releaseDate\":"
            + release.getReleaseDate().getTime()
            + ",\"dictionaryVersion\":\"0.6e\",\"submissions\":[{\"projectKey\":\"project1\",\"projectName\":\"project one\",\"releaseName\":\"ICGC13\"},"
            + "{\"projectKey\":\"project2\",\"projectName\":\"project two\",\"releaseName\":\"ICGC13\"}]}]"));
  }

}
