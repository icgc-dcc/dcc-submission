package org.icgc.dcc.model;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.service.ReleaseService;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.inject.Inject;
import com.mysema.query.mongodb.MongodbQuery;
import com.mysema.query.mongodb.morphia.MorphiaQuery;
import com.mysema.query.types.Predicate;

// TODO: make an abstract base class
public class Projects {

  private final Morphia morphia;

  private final Datastore datastore;

  @Inject
  public Projects(Morphia morphia, Datastore datastore) {
    super();

    checkArgument(morphia != null);
    checkArgument(datastore != null);

    this.morphia = morphia;
    this.datastore = datastore;
  }

  public Datastore datastore() {
    return datastore;
  }

  public MongodbQuery<Project> query() {
    return new MorphiaQuery<Project>(morphia, datastore, QProject.project);
  }

  public MongodbQuery<Project> where(Predicate predicate) {
    return query().where(predicate);
  }

  public List<Release> getReleases(Project project) {
    MorphiaQuery<Release> releaseQuery = new MorphiaQuery<Release>(morphia, datastore, QRelease.release);
    List<Release> releases = new ArrayList<Release>();
    for(Release release : releaseQuery.list()) {
      for(Submission submission : release.getSubmissions()) {
        if(submission.getAccessionId().equals(project.getAccessionId())) {
          releases.add(release);
          continue;
        }
      }
    }

    return releases;
  }

  @SuppressWarnings("all")
  public void addProject(Project project) {
    this.saveProject(project);

    // TODO: this will actually need to throw an event and let DccFilesystem catch it so it can perform the following:
    if(false) {
      ReleaseService releaseService = null;
      DccFileSystem dccFilesystem = null;
      Release release = releaseService.getNextRelease().getRelease();
      dccFilesystem.mkdirProjectDirectory(release, project);
    }
  }

  public List<Project> getProjects() {
    return this.query().list();
  }

  public void saveProject(Project project) {
    this.datastore().save(project);
  }
}
