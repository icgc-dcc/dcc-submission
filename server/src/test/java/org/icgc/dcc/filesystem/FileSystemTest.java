package org.icgc.dcc.filesystem;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.core.model.User;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.junit.Before;

import com.typesafe.config.Config;

public class FileSystemTest {

  protected static final String USERNAME = "vegeta";

  protected static final String ROOT_DIR = "/tmp/my_root_fs_dir";

  protected static final String RELEASE_NAME = "ICGC4";

  protected static final String PROJECT_NAME = "dragon_balls_quest";

  protected static final String PROJECT_KEY = "DBQ";

  protected static final String FILENAME_1 = "cnsm__bla__bla__p__bla__bla.tsv";

  protected static final String FILENAME_2 = "cnsm__bla__bla__s__bla__bla.tsv";

  protected static final String FILEPATH_1 = ROOT_DIR + RELEASE_NAME + "/" + PROJECT_KEY + "/" + FILENAME_1;

  protected static final String FILEPATH_2 = ROOT_DIR + RELEASE_NAME + "/" + PROJECT_KEY + "/" + FILENAME_2;

  protected Release mockRelease;

  protected User mockUser;

  protected Project mockProject;

  protected Submission mockSubmission;

  protected Config mockConfig;

  static {
    setProperties();
  }

  /**
   * Sets key system properties before test initialization.
   */
  private static void setProperties() {
    // See http://stackoverflow.com/questions/7134723/hadoop-on-osx-unable-to-load-realm-info-from-scdynamicstore
    System.setProperty("java.security.krb5.realm", "OX.AC.UK");
    System.setProperty("java.security.krb5.kdc", "kdc0.ox.ac.uk:kdc1.ox.ac.uk");
  }

  @Before
  public void setUp() throws IOException {

    this.mockConfig = mock(Config.class);
    this.mockRelease = mock(Release.class);
    this.mockUser = mock(User.class);
    this.mockProject = mock(Project.class);
    this.mockSubmission = mock(Submission.class);

    when(this.mockConfig.getString(FsConfig.FS_ROOT)).thenReturn(ROOT_DIR);

    when(this.mockRelease.getName()).thenReturn(RELEASE_NAME);

    when(this.mockUser.getName()).thenReturn(USERNAME);

    when(this.mockProject.getName()).thenReturn(PROJECT_NAME);
    when(this.mockProject.getKey()).thenReturn(PROJECT_KEY);
    when(this.mockProject.hasUser(this.mockUser.getName())).thenReturn(true);

    when(this.mockSubmission.getState()).thenReturn(SubmissionState.SIGNED_OFF);

    when(this.mockRelease.getSubmission(this.mockProject.getKey())).thenReturn(this.mockSubmission);
    List<String> projectKeys = Arrays.asList(this.mockProject.getKey()); // must be separated from thenReturn call
                                                                         // (mockito bug:
                                                                         // http://code.google.com/p/mockito/issues/detail?id=53)
    when(this.mockRelease.getProjectKeys()).thenReturn(projectKeys);
  }
}
