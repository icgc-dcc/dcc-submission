package org.icgc.dcc.submission.services;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import lombok.val;

import org.bson.types.ObjectId;
import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.repository.ProjectRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.code.morphia.Key;
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
  public void testFindAll() throws Exception {
    val expected = Sets.newHashSet(projectOne, projectTwo);
    when(projectRepository.findAll()).thenReturn(expected);

    val actual = projectService.findAll();

    verify(projectRepository).findAll();
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testFindAllForUser() throws Exception {
    val expected = Sets.newHashSet(projectOne);
    when(projectRepository.findAllForUser(any(String.class))).thenReturn(expected);

    val actual = projectService.findAllForUser(any(String.class));

    verify(projectRepository).findAllForUser(any(String.class));
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testFind() throws Exception {
    val expected = projectOne;
    when(projectRepository.find(projectOne.getKey())).thenReturn(expected);

    val actual = projectService.find(projectOne.getKey());

    verify(projectRepository).find(projectOne.getKey());
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testFindForUser() throws Exception {
    val expected = projectOne;
    when(projectRepository.findForUser(projectOne.getKey(), "username")).thenReturn(expected);

    val actual = projectService.findForUser(projectOne.getKey(), "username");

    verify(projectRepository).findForUser(projectOne.getKey(), "username");
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void testAddProject() throws Exception {
    val expected = new Key<Project>(Project.class, new ObjectId());
    when(projectRepository.upsert(projectOne)).thenReturn(expected);

    val response = projectService.add(projectOne);

    verify(projectRepository).upsert(projectOne);
    assertThat(response).isEqualTo(expected);
  }

  @Test
  public void testUpdateProject() throws Exception {
    val expected = new Key<Project>(Project.class, new ObjectId());
    when(projectRepository.upsert(projectOne)).thenReturn(expected);

    val response = projectService.update(projectOne);

    verify(projectRepository).upsert(projectOne);
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

    val actual = projectService.clean(dirty);

    assertThat(actual).isEqualTo(expected);
  }

}
