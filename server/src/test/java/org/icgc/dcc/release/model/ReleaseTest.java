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

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
    assertEquals(0, release.getQueue().size());
    String projectKey = "pkey";
    release.enqueue(projectKey);
    assertEquals(1, release.getQueue().size());
    assertEquals("pkey", release.getQueue().get(0));
  }

  @Test
  public void test_enqueue_oneKey_null() {
    assertEquals(0, release.getQueue().size());
    String projectKey = null;
    release.enqueue(projectKey);
    assertEquals(0, release.getQueue().size());
  }

  @Test
  public void test_enqueue_oneKey_emptyString() {
    assertEquals(0, release.getQueue().size());
    String projectKey = "";
    release.enqueue(projectKey);
    assertEquals(0, release.getQueue().size());
  }

  @Test
  public void test_enqueue_manyKeys() {
    assertEquals(0, release.getQueue().size());
    List<String> projectKeys = new ArrayList<String>();
    projectKeys.add("pkey1");
    projectKeys.add("pkey2");
    release.enqueue(projectKeys);
    assertEquals(2, release.getQueue().size());
    assertEquals("pkey1", release.getQueue().get(0));
  }

  @Test
  public void test_enqueue_manyKeys_null() {
    assertEquals(0, release.getQueue().size());
    List<String> projectKeys = new ArrayList<String>();
    projectKeys.add(null);
    projectKeys.add("pkey");
    release.enqueue(projectKeys);
    assertEquals(1, release.getQueue().size());
    assertEquals("pkey", release.getQueue().get(0));
  }

  @Test
  public void test_enqueue_manyKeys_emptyString() {
    assertEquals(0, release.getQueue().size());
    List<String> projectKeys = new ArrayList<String>();
    projectKeys.add("");
    projectKeys.add("pkey");
    release.enqueue(projectKeys);
    assertEquals(1, release.getQueue().size());
    assertEquals("pkey", release.getQueue().get(0));
  }

  @Test
  public void test_dequeue() {
    assertEquals(0, release.getQueue().size());
    List<String> projectKeys = new ArrayList<String>();
    projectKeys.add("pkey1");
    projectKeys.add("pkey2");
    release.enqueue(projectKeys);
    assertEquals(2, release.getQueue().size());
    assertEquals("pkey1", release.dequeue());
    assertEquals(1, release.getQueue().size());
    assertEquals("pkey2", release.getQueue().get(0));
  }

  @Test
  public void test_emptyQueue() {
    assertEquals(0, release.getQueue().size());
    String projectKey = "pkey";
    release.enqueue(projectKey);
    assertEquals(1, release.getQueue().size());
    release.emptyQueue();
    assertEquals(0, release.getQueue().size());
  }
}
