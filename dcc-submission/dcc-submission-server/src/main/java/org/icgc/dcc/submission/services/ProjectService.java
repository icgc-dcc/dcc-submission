package org.icgc.dcc.submission.services;

import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.shiro.subject.Subject;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.repository.ProjectRepository;

import com.google.inject.Inject;

@Slf4j
public class ProjectService {

  @Inject
  private final ProjectRepository projectRepository;

  @Inject
  public ProjectService(ProjectRepository projectRepository) {
    this.projectRepository = projectRepository;
  }

  public Project findProject(String projectKey) {
    log.info("Passing on request for Project {}", projectKey);
    return projectRepository.findProject(projectKey);
  }

  public Set<Project> findProjects() {
    log.info("Passing on request for all Projects");
    return projectRepository.findProjects();
  }

  public Set<Project> findProjects(Subject user) {
    log.info("Passing on request for all Projects for {}", user.getPrincipal());
    return projectRepository.findProjects(user);
  }

  public void addProject(Project project) {
    log.info("Passing on request for adding Project {}", project);
    projectRepository.addProject(project);
  }
}
