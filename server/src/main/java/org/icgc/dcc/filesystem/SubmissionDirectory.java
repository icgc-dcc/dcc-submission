package org.icgc.dcc.filesystem;

import static com.google.common.base.Preconditions.checkArgument;

import java.io.File;
import java.io.InputStream;
import java.util.regex.Pattern;

import org.icgc.dcc.model.Project;
import org.icgc.dcc.model.Release;

import com.google.inject.Inject;

public class SubmissionDirectory {

  @SuppressWarnings("unused")
  private final Release release;

  @SuppressWarnings("unused")
  private final Project project;

  private final ReleaseFilesystem parent;

  @Inject
  public SubmissionDirectory(ReleaseFilesystem parent, Release release, Project project) {

    checkArgument(parent != null);
    checkArgument(release != null);
    checkArgument(project != null);

    this.parent = parent;
    this.release = release;
    this.project = project;
  }

  public Iterable<File> listFile(Pattern pattern) {
    return null;
  }

  public void addFile(InputStream data) {

  }

  public void deleteFile(String filename) {

  }

  public boolean isReadOnly() {
    return this.parent.isReadOnly();
  }

}
