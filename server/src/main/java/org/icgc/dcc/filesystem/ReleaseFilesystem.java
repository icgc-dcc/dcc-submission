package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import org.icgc.dcc.model.Project;
import org.icgc.dcc.model.Release;
import org.icgc.dcc.model.User;

public class ReleaseFilesystem {

  @SuppressWarnings("unused")
  private final Release release;

  @SuppressWarnings("unused")
  private final User user;

  @SuppressWarnings("unused")
  private final boolean readOnly;

  @SuppressWarnings("unused")
  private List<SubmissionDirectory> submissionDirectoryList;

  public ReleaseFilesystem(Release release, User user) {
    checkArgument(release != null);
    checkArgument(user != null);
    this.release = release;
    this.user = user;
    this.readOnly = false; // until set differently by DCC admins
  }

  public boolean isReadOnly() {
    return false;
  }

  public Iterable<SubmissionDirectory> listSubmissionDirectory() {
    return null;
  }

  public SubmissionDirectory getSubmissionDirectory(Project project) {
    return null;
  }
}
