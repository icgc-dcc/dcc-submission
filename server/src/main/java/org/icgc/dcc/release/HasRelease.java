package org.icgc.dcc.release;

import java.util.List;

import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.core.model.User;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.release.model.Release;

public interface HasRelease {

  public ReleaseFileSystem getReleaseFilesystem(User user);

  public ReleaseFileSystem getReleaseFilesystem();

  public Release getRelease();

  public List<Project> getProjects();
}
