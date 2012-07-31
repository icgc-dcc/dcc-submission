package org.icgc.dcc.core;

import java.util.ArrayList;
import java.util.List;

import org.icgc.dcc.core.model.Project;
import org.icgc.dcc.core.model.QProject;
import org.icgc.dcc.core.morphia.BaseMorphiaService;
import org.icgc.dcc.filesystem.DccFileSystem;
import org.icgc.dcc.release.ReleaseService;
import org.icgc.dcc.release.model.QRelease;
import org.icgc.dcc.release.model.Release;
import org.icgc.dcc.release.model.Submission;
import org.icgc.dcc.release.model.SubmissionState;
import org.icgc.dcc.web.validator.NameValidator;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;
import com.google.code.morphia.query.Query;
import com.google.code.morphia.query.UpdateOperations;
import com.google.inject.Inject;
import com.mysema.query.mongodb.morphia.MorphiaQuery;

public class ProjectService extends BaseMorphiaService<Project> {

  private final DccFileSystem fs;

  private final ReleaseService releaseService;

  @Inject
  public ProjectService(Morphia morphia, Datastore datastore, DccFileSystem fs, ReleaseService releaseService) {
    super(morphia, datastore, QProject.project);
    super.registerModelClasses(Project.class);
    this.fs = fs;
    this.releaseService = releaseService;
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
      throw new ProjectServiceException("Project key " + project.getKey() + " is not valid");
    }
    Release release = releaseService.getNextRelease().getRelease();
    Submission submission = new Submission();
    submission.setProjectKey(project.getKey());
    submission.setState(SubmissionState.NOT_VALIDATED);
    release.addSubmission(submission);
    fs.mkdirProjectDirectory(release, project.getKey());

    Query<Release> updateQuery = datastore().createQuery(Release.class)//
        .filter("name = ", release.getName());
    UpdateOperations<Release> ops = datastore().createUpdateOperations(Release.class).add("submissions", submission);
    datastore().update(updateQuery, ops);

    this.saveProject(project);
  }

  public List<Project> getProjects() {
    return this.query().list();
  }

  public Project getProject(final String projectKey) {
    Project project = this.datastore().createQuery(Project.class).filter("key =", projectKey).get();
    if(project == null) {
      throw new ProjectServiceException("No project found with key " + projectKey);
    }
    return project;
  }

  public List<Project> getProjects(List<String> projectKeys) {
    List<Project> projects = this.datastore().createQuery(Project.class).filter("key in", projectKeys).asList();
    if(projects == null) {
      throw new ProjectServiceException("No projects found within the key list");
    }
    return projects;
  }

  private void saveProject(Project project) {
    this.datastore().save(project);
  }
}
