package org.icgc.dcc.submission.service;

import java.util.List;
import java.util.Set;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.repository.ProjectRepository;

import com.google.code.morphia.Key;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

@Slf4j
@RequiredArgsConstructor(onConstructor = @_(@Inject))
public class ProjectService {

  @NonNull
  private final ProjectRepository projectRepository;

  public List<Project> getProjects() {
    log.debug("Request to find all Projects");
    return projectRepository.findProjects();
  }

  public Project getProject(String projectKey) {
    log.info("Request for Project '{}'", projectKey);
    return projectRepository.findProject(projectKey);
  }

  public Project getProjectByUser(String projectKey, String username) {
    log.debug("Request for Project '{}' for ", projectKey, username);
    return projectRepository.findProjectByUser(projectKey, username);
  }

  public List<Project> getProjectsByUser(String username) {
    log.debug("Request to find Projects for User '{}'", username);
    return projectRepository.findProjectsByUser(username);
  }

  public Key<Project> addProject(Project project) {
    log.info("Adding Project '{}'", project);
    return projectRepository.upsertProject(project);
  }

  public Key<Project> updateProject(Project project) {
    log.info("Updating Project '{}'", project);
    return projectRepository.upsertProject(cleanProject(project));
  }

  public Project cleanProject(Project dirty) {
    log.info("Cleaning Project '{}'", dirty);
    val clean = new Project(dirty.getKey(), dirty.getName());
    clean.setAlias(dirty.getAlias());

    log.info("Returing cleaned Project '{}'", clean);
    return clean;
  }

  public Set<Submission> extractSubmissions(Iterable<Release> releases, String projectKey) {
    val submissions = Sets.<Submission> newHashSet();

    for (val release : releases) {
      val optional = release.getSubmissionByProjectKey(projectKey);
      if (optional.isPresent()) {
        val submission = optional.get();
        submissions.add(submission);
      } else {
        log.info("Submission for project '{}' not found in release '{}'", projectKey, release.getName());
      }
    }

    return submissions;
  }

}
