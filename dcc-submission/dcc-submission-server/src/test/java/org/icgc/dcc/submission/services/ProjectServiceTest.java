package org.icgc.dcc.submission.services;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import lombok.val;

import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.repository.ProjectRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
public class ProjectServiceTest {

  private ProjectService projectService;

  @Mock
  private ProjectRepository projectRepository;

  private final Project projectOne = new Project("PRJ1", "Project One");

  private final Project projectTwo = new Project("PRJ2", "Project Two");

  @Before
  public void setUp() throws Exception {
    projectService = new ProjectService(projectRepository);
  }

  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void testFindProjects() throws Exception {
    val expected = Sets.newHashSet(projectOne, projectTwo);
    when(projectRepository.findProjects()).thenReturn(expected);

    val actual = projectService.findProjects();

    verify(projectRepository).findProjects();
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testFindProjectsForUser() throws Exception {
    val expected = Sets.newHashSet(projectOne);
    when(projectRepository.findProjectsForUser(any(String.class))).thenReturn(expected);

    val actual = projectService.findProjectsForUser(any(String.class));

    verify(projectRepository).findProjectsForUser(any(String.class));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testFindProject() throws Exception {
    val expected = projectOne;
    when(projectRepository.findProject(projectOne.getKey())).thenReturn(expected);

    val actual = projectService.findProject(projectOne.getKey());

    verify(projectRepository).findProject(projectOne.getKey());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testFindProjectForUser() throws Exception {
    val expected = projectOne;
    when(projectRepository.findProjectForUser(projectOne.getKey(), "username")).thenReturn(expected);

    val actual = projectService.findProjectForUser(projectOne.getKey(), "username");

    verify(projectRepository).findProjectForUser(projectOne.getKey(), "username");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testAddProject() throws Exception {
    val expected = Sets.newHashSet(projectOne, projectTwo);
    when(projectRepository.findProjects()).thenReturn(expected);

    projectService.addProject(projectOne);

    verify(projectRepository).upsertProject(projectOne);
  }
}
