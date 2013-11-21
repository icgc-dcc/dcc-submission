package org.icgc.dcc.submission.services;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import lombok.val;

import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.repository.ReleaseRepository;
import org.icgc.dcc.submission.service.ReleaseService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
public class ReleaseServiceTest {

  private ReleaseService releaseService;

  @Mock
  private ReleaseRepository releaseRepository;

  private Release release;

  @Before
  public void setUp() throws Exception {
    release = new Release("R1");
    releaseService = new ReleaseService(releaseRepository);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testFind() throws Exception {
    val expected = release;
    when(releaseRepository.find(release.getName())).thenReturn(expected);

    val actual = releaseService.find(release.getName());

    verify(releaseRepository).find(release.getName());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testFindAll() throws Exception {
    val expected = Sets.newHashSet(release);
    when(releaseRepository.findAll()).thenReturn(expected);

    val actual = releaseService.findAll();

    verify(releaseRepository).findAll();
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testFindOpen() throws Exception {
    val expected = release;
    when(releaseRepository.findOpen()).thenReturn(expected);

    val actual = releaseService.findOpen();

    verify(releaseRepository).findOpen();
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testAddSubmission() throws Exception {
    val expected = release;
    val submission = new Submission("key", "value", release.getName());

    when(releaseRepository.addSubmission(submission, release.getName())).thenReturn(expected);
    when(releaseRepository.findOpen()).thenReturn(expected);

    val actual = releaseService.addSubmission(submission.getProjectKey(), submission.getProjectName());

    verify(releaseRepository).addSubmission(submission, release.getName());

    assertThat(actual).isEqualTo(expected);
  }

}
