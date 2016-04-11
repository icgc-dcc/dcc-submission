package org.icgc.dcc.submission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.bson.types.ObjectId;
import org.elasticsearch.common.collect.Lists;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.repository.ProjectRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mongodb.morphia.Key;

import com.google.common.collect.Sets;

import lombok.val;

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
  public void testFindAll() throws Exception {
    val expected = Lists.newArrayList(projectOne, projectTwo);
    when(projectRepository.findProjects()).thenReturn(expected);

    val actual = projectService.getProjects();

    verify(projectRepository).findProjects();
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testFindAllForUser() throws Exception {
    val expected = Lists.newArrayList(projectOne);
    when(projectRepository.findProjectsByUser(any(String.class))).thenReturn(expected);

    val actual = projectService.getProjectsByUser(any(String.class));

    verify(projectRepository).findProjectsByUser(any(String.class));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testFind() throws Exception {
    val expected = projectOne;
    when(projectRepository.findProject(projectOne.getKey())).thenReturn(expected);

    val actual = projectService.getProject(projectOne.getKey());

    verify(projectRepository).findProject(projectOne.getKey());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testFindForUser() throws Exception {
    val expected = projectOne;
    when(projectRepository.findProjectByUser(projectOne.getKey(), "username")).thenReturn(expected);

    val actual = projectService.getProjectByUser(projectOne.getKey(), "username");

    verify(projectRepository).findProjectByUser(projectOne.getKey(), "username");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testAddProject() throws Exception {
    val expected = new Key<Project>(Project.class, new ObjectId());
    when(projectRepository.upsertProject(projectOne)).thenReturn(expected);

    val response = projectService.addProject(projectOne);

    verify(projectRepository).upsertProject(projectOne);
    assertThat(response).isEqualTo(expected);
  }

  @Test
  public void testUpdateProject() throws Exception {
    val expected = new Key<Project>(Project.class, new ObjectId());
    when(projectRepository.upsertProject(projectOne)).thenReturn(expected);

    val response = projectService.updateProject(projectOne);

    verify(projectRepository).upsertProject(projectOne);
    assertThat(response).isEqualTo(expected);
  }

  @Test
  public void testClean() throws Exception {
    val dirty = new Project("PK", "PN");
    dirty.setAlias("PA");
    dirty.setGroups(Sets.newHashSet("group1", "group2"));
    dirty.setUsers(Sets.newHashSet("group1", "group2"));

    val expected = new Project("PK", "PN");
    expected.setAlias("PA");

    val actual = projectService.cleanProject(dirty);

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testExtractSubmissions() throws Exception {
    val releaseOne = new Release("R1");
    val releaseTwo = new Release("R2");
    val releaseThree = new Release("R3");
    val submissionOne = new Submission(projectOne.getKey(), projectOne.getName(), releaseOne.getName());
    val submissionTwo = new Submission(projectOne.getKey(), projectOne.getName(), releaseTwo.getName());

    releaseOne.addSubmission(submissionOne);
    releaseTwo.addSubmission(submissionTwo);

    val releases = Sets.newHashSet(releaseOne, releaseTwo, releaseThree);

    val expected = Sets.newHashSet(submissionOne, submissionTwo);

    val actual = projectService.getSubmissions(releases, projectOne.getKey());

    assertThat(actual).isEqualTo(expected);
  }

}
