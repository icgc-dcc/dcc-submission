package org.icgc.dcc.release;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;

import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.core.model.QProject;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.filesystem.ReleaseFileSystem;
import org.icgc.dcc.release.model.Release;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.common.collect.ImmutableList;
import com.mysema.query.mongodb.morphia.MorphiaQuery;

public abstract class BaseRelease implements HasRelease {

  private final Datastore datastore;

  private final Morphia morphia;

  private final DccFileSystem dccFilesystem;

  private final Release release;

  protected BaseRelease(Release release, Morphia morphia, Datastore datastore, DccFileSystem fs) {
    checkArgument(release != null);
    checkArgument(morphia != null);
    checkArgument(datastore != null);
    checkArgument(fs != null);
    this.release = release;
    this.morphia = morphia;
    this.datastore = datastore;
    this.dccFilesystem = fs;
  }

  @Override
  public ReleaseFileSystem getReleaseFilesystem() {
    return this.dccFilesystem.getReleaseFilesystem(this.release);
  }

  @Override
  public List<Project> getProjects() {
    return new MorphiaQuery<Project>(morphia(), datastore(), QProject.project).where(
        QProject.project.key.in(ImmutableList.copyOf(getRelease().getProjectKeys()))).list();
  }

  @Override
  public Release getRelease() {
    return release;
  }

  protected Morphia morphia() {
    return morphia;
  }

  protected Datastore datastore() {
    return datastore;
  }

  protected DccFileSystem fileSystem() {
    return dccFilesystem;
  }

}
