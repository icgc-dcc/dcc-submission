package org.icgc.dcc.core;

import java.util.ArrayList;
import java.util.List;

import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.core.model.QProject;
import org.icgc.dcc.core.morphia.BaseMorphiaService;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.release.model.QRelease;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.ReleaseState;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.icgc.dcc.web.validator.InvalidNameException;
import org.icgc.dcc.web.validator.NameValidator;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.inject.Inject;
import com.mysema.query.mongodb.morphia.MorphiaQuery;

public class ProjectService extends BaseMorphiaService<Project> {

  private final DccFileSystem fs;

  @Inject
  public ProjectService(Morphia morphia, Datastore datastore, DccFileSystem fs) {
    super(morphia, datastore, QProject.project);
    super.registerModelClasses(Project.class);
    this.fs = fs;
  }

  public List<Release> getReleases(Project project) {
    MorphiaQuery<Release> releaseQuery = new MorphiaQuery<Release>(morphia(), datastore(), QRelease.release);
    List<Release> releases = new ArrayList<Release>();
    for(Release release : releaseQuery.list()) {
      for(Submission submission : release.getSubmissions()) {
        if(submission.getProjectKey().equals(project.getKey())) {
          releases.add(release);
          continue;
        }
      }
    }

    return releases;
  }

  @SuppressWarnings("all")
  public void addProject(Project project) {
    // check for project key
    if(!NameValidator.validate(project.getKey())) {
      throw new InvalidNameException(project.getKey());
    }

    this.saveProject(project);

    MorphiaQuery<Release> releaseQuery = new MorphiaQuery<Release>(morphia(), datastore(), QRelease.release);
    Release release = releaseQuery.where(QRelease.release.state.eq(ReleaseState.OPENED)).singleResult();
    Submission submission = new Submission();
    submission.setProjectKey(project.getKey());
    submission.setState(SubmissionState.NOT_VALIDATED);
    release.addSubmission(submission);
    fs.mkdirProjectDirectory(release, project.getKey());

    Query<Release> updateQuery = datastore().createQuery(Release.class)//
        .filter("name = ", release.getName());
    UpdateOperations<Release> ops = datastore().createUpdateOperations(Release.class).add("submissions", submission);
    datastore().update(updateQuery, ops);
  }

  public List<Project> getProjects() {
    return this.query().list();
  }

  public Project getProject(final String projectKey) {
    Project project = this.query().where(QProject.project.key.eq(projectKey)).singleResult();
    if(project == null) {
      throw new ProjectServiceException("No project found with key " + projectKey);
    }
    return project;
  }

  public List<Project> getProjects(final List<String> projectKeys) {
    return this.query().where(QProject.project.key.in(projectKeys)).list();
  }

  private void saveProject(Project project) {
    this.datastore().save(project);
  }
}
