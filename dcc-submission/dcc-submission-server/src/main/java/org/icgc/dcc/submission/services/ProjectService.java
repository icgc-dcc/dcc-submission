package org.icgc.dcc.submission.services;

import java.util.Set;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.repository.ProjectRepository;

@Slf4j
@RequiredArgsConstructor
public class ProjectService {

  @NonNull
  private final ProjectRepository projectRepository;

  public Project findProject(String projectKey) {
    log.info("Passing on request for Project [{}]", projectKey);
    return projectRepository.findProject(projectKey);
  }

  public Project findProject(String projectKey, String username) {
    log.info("Passing on request for Project [{}] for ", projectKey, username);
    return projectRepository.findProject(projectKey, username);
  }

  public Set<Project> findProjects() {
    log.info("Passing on request to find all Projects");
    return projectRepository.findProjects();
  }

  public Set<Project> findProjects(String username) {
    log.info("Passing on request to find Projects for User [{}]", username);
    return projectRepository.findProjects(username);
  }

  public void addProject(Project project) {
    log.info("Passing on request to add Project [{}]", project);
    projectRepository.addProject(project);
  }
}
