package org.icgc.dcc.submission.repository;

import org.icgc.dcc.submission.core.MailService;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.code.morphia.Datastore;
import com.google.code.morphia.Morphia;

@RunWith(MockitoJUnitRunner.class)
public class ProjectRepositoryTest {

  @Mock
  private Datastore datastore;

  @Mock
  private MailService mailService;

  @Mock
  private Morphia morphia;
  @InjectMocks
  private ProjectRepository projectRepository;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  @Before
  public void setUp() throws Exception {
  }

  @After
  public void tearDown() throws Exception {
  }

}
