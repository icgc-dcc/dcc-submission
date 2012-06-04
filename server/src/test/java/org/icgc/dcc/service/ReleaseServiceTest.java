package org.icgc.dcc.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.icgc.dcc.model.Project;
import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.Submission;
import org.icgc.dcc.model.SubmissionState;
import org.junit.Test;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.mongodb.Mongo;

public class ReleaseServiceTest {

  @Test
  public void test() {

    Mongo mongo = mock(Mongo.class);
    Morphia morphia = mock(Morphia.class);
    Datastore ds = morphia.createDatastore(mongo, "testDB");

    Release release = mock(Release.class);
    Project project = mock(Project.class);
    Submission submission = mock(Submission.class);

    submission.setState(SubmissionState.VALID);
    submission.setProject(project);

    release.getSubmissions().add(submission);

    ReleaseService releaseService = new ReleaseService(morphia, ds);

    when(releaseService.getNextRelease().getRelease()).thenReturn(release);
    when(releaseService.getCompletedReleases().size()).thenReturn(0);
    when(releaseService.list().size()).thenReturn(1);

    Release newRelease = mock(Release.class);
    releaseService.getNextRelease().release(newRelease);

    when(releaseService.getNextRelease().getRelease()).thenReturn(newRelease);
    when(releaseService.getCompletedReleases().size()).thenReturn(1);
    when(releaseService.list().size()).thenReturn(2);

  }

}
