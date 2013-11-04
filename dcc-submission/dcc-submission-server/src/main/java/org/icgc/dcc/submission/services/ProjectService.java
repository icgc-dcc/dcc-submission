package org.icgc.dcc.submission.services;

import java.util.Set;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.repository.ProjectRepository;

import com.google.code.morphia.Key;

@Slf4j
@RequiredArgsConstructor
public class ProjectService {

  @NonNull
  private final ProjectRepository projectRepository;

  public Project find(String projectKey) {
    log.info("Passing on request for Project [{}]", projectKey);
    return projectRepository.find(projectKey);
  }

  public Project findForUser(String projectKey, String username) {
    log.info("Passing on request for Project [{}] for ", projectKey, username);
    return projectRepository.findForUser(projectKey, username);
  }

  public Set<Project> findAll() {
    log.info("Passing on request to find all Projects");
    return projectRepository.findAll();
  }

  public Set<Project> findAllForUser(String username) {
    log.info("Passing on request to find Projects for User [{}]", username);
    return projectRepository.findAllForUser(username);
  }

  public Key<Project> upsert(Project project) {
    log.info("Passing on request to upsert Project [{}]", project);
    return projectRepository.upsert(project);
  }
}
