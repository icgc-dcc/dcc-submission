/**
 * Copyright 2012(c) The Ontario Institute for Cancer Research. All rights reserved.
 *
 * This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
 * You should have received a copy of the GNU General Public License along with 
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT 
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED 
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN 
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.icgc.dcc.release.model;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * 
 */
public class ReleaseTest {

  /**
   * @throws java.lang.Exception
   */
  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @AfterClass
  public static void tearDownAfterClass() throws Exception {
  }

  private final Release release = new Release();

  private static final String EMAIL = "a@a.com";

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @After
  public void tearDown() throws Exception {
  }

  @Test
  public void test_addSubmission() {
    assertEquals(0, release.getSubmissions().size());
    Submission submission = new Submission();
    release.addSubmission(submission);
    assertEquals(1, release.getSubmissions().size());
  }

  @Test
  public void test_enqueue_oneKey() {
    assertEquals(0, release.getQueuedProjectKeys().size());
    String projectKey = "pkey";
    release.enqueue(new QueuedProject(projectKey, Lists.newArrayList(EMAIL)));
    assertEquals(1, release.getQueuedProjectKeys().size());
    assertEquals("pkey", release.getQueuedProjectKeys().get(0));
  }

  @Test
  public void test_enqueue_oneKey_null() {
    assertEquals(0, release.getQueuedProjectKeys().size());
    String projectKey = null;
    release.enqueue(new QueuedProject(projectKey, Lists.newArrayList(EMAIL)));
    assertEquals(0, release.getQueuedProjectKeys().size());
  }

  @Test
  public void test_enqueue_oneKey_emptyString() {
    assertEquals(0, release.getQueuedProjectKeys().size());
    String projectKey = "";
    release.enqueue(new QueuedProject(projectKey, Lists.newArrayList(EMAIL)));
    assertEquals(0, release.getQueuedProjectKeys().size());
  }

  @Test
  public void test_enqueue_manyKeys() {
    assertEquals(0, release.getQueuedProjectKeys().size());
    List<QueuedProject> queuedProjects = Lists.newArrayList();
    queuedProjects.add(new QueuedProject("pkey1", Lists.newArrayList(EMAIL)));
    queuedProjects.add(new QueuedProject("pkey2", Lists.newArrayList(EMAIL)));
    release.enqueue(queuedProjects);
    assertEquals(2, release.getQueuedProjectKeys().size());
    assertEquals("pkey1", release.getQueuedProjectKeys().get(0));
  }

  @Test
  public void test_enqueue_manyKeys_null() {
    assertEquals(0, release.getQueuedProjectKeys().size());
    List<QueuedProject> queuedProjects = Lists.newArrayList();
    queuedProjects.add(new QueuedProject(null, Lists.newArrayList(EMAIL)));
    queuedProjects.add(new QueuedProject("pkey", Lists.newArrayList(EMAIL)));
    release.enqueue(queuedProjects);
    assertEquals(1, release.getQueuedProjectKeys().size());
    assertEquals("pkey", release.getQueuedProjectKeys().get(0));
  }

  @Test
  public void test_enqueue_manyKeys_emptyString() {
    assertEquals(0, release.getQueuedProjectKeys().size());
    List<QueuedProject> queuedProjects = Lists.newArrayList();
    queuedProjects.add(new QueuedProject("", Lists.newArrayList(EMAIL)));
    queuedProjects.add(new QueuedProject("pkey", Lists.newArrayList(EMAIL)));
    release.enqueue(queuedProjects);
    assertEquals(1, release.getQueuedProjectKeys().size());
    assertEquals("pkey", release.getQueuedProjectKeys().get(0));
  }

  @Test
  public void test_dequeue() {
    assertEquals(0, release.getQueuedProjectKeys().size());
    List<QueuedProject> queuedProjects = Lists.newArrayList();
    queuedProjects.add(new QueuedProject("pkey1", Lists.newArrayList(EMAIL)));
    queuedProjects.add(new QueuedProject("pkey2", Lists.newArrayList(EMAIL)));
    release.enqueue(queuedProjects);
    assertEquals(2, release.getQueuedProjectKeys().size());
    assertEquals("pkey1", release.dequeue().get().getKey());
    assertEquals(1, release.getQueuedProjectKeys().size());
    assertEquals("pkey2", release.getQueuedProjectKeys().get(0));
  }

  @Test
  public void test_emptyQueue() {
    assertEquals(0, release.getQueuedProjectKeys().size());
    String projectKey = "pkey";
    release.enqueue(new QueuedProject(projectKey, Lists.newArrayList(EMAIL)));
    assertEquals(1, release.getQueuedProjectKeys().size());
    release.emptyQueue();
    assertEquals(0, release.getQueuedProjectKeys().size());
  }
}
