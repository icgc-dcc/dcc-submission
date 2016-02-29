/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.loader.db.postgres;

import static org.assertj.core.api.Assertions.assertThat;
import static org.icgc.dcc.submission.loader.util.Services.createSubmissionService;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.loader.meta.TypeDefGraph;
import org.icgc.dcc.submission.loader.model.Project;
import org.icgc.dcc.submission.loader.util.AbstractPostgressTest;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

@Slf4j
public class PostgresDatabaseServiceTest extends AbstractPostgressTest {

  PostgresDatabaseService service;

  @Before
  @Override
  public void setUp() throws Exception {
    super.setUp();
    val submissionService = createSubmissionService();
    val graph = new TypeDefGraph(submissionService.getFileTypes());
    this.service = new PostgresDatabaseService(submissionService, jdbcTemplate, graph);
  }

  @Test
  public void testInitializeDb() throws Exception {
    val release = "icgc21";
    service.initializeDb(release, createProjects());
    val sql = "select * from " + release + ".project";
    val projects = jdbcTemplate.queryForList(sql);
    log.debug("{}", projects);
    assertThat(projects).hasSize(2);
  }

  private Iterable<Project> createProjects() {
    return ImmutableList.of(
        new Project("ALL-US", "ALL-US", SubmissionState.VALID),
        new Project("PACA-US", "PACA-US", SubmissionState.VALID));
  }

}
