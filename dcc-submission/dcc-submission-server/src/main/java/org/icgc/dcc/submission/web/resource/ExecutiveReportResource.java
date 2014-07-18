/*
 * Copyright (c) 2014 The Ontario Institute for Cancer Research. All rights reserved.                             
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
package org.icgc.dcc.submission.web.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.repository.CodeListRepository;
import org.icgc.dcc.submission.repository.DictionaryRepository;
import org.icgc.dcc.submission.service.ExecutiveReportService;
import org.icgc.dcc.submission.service.ReleaseService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

@Slf4j
@Path("executive_report")
public class ExecutiveReportResource {

  @Inject
  private ExecutiveReportService service;

  // test
  @Inject
  private ReleaseService releaseService;
  @Inject
  private DictionaryRepository dictionaryRepository;
  @Inject
  private CodeListRepository codelistRepository;
  @Inject
  private DccFileSystem dccFileSystem;

  @GET
  @Path("/test")
  public Response testStuff() {

    ObjectMapper mapper = new ObjectMapper();
    final String _releaseName = "release1";
    val _projectKeys = ImmutableSet.<String> of("project.1");
    val release = releaseService.getReleaseByName("release1");
    log.info(release.getDictionaryVersion());

    val dictionaryNode =
        mapper.valueToTree(dictionaryRepository.findDictionaryByVersion(release.getDictionaryVersion()));
    val codelistNode = mapper.valueToTree(codelistRepository.findCodeLists());
    val releasePath = dccFileSystem.buildReleaseStringPath(_releaseName);

    // This spawns a separate thread
    service.generateReport(_releaseName, _projectKeys, releasePath, dictionaryNode, codelistNode);
    return Response.ok("Stuff should be running in the background still").build();
  }

  @GET
  @Path("{releaseName}/{project}")
  public Response getReport(
      @PathParam("releaseName") String releaseName,
      @PathParam("project") String projects
      ) {

    // FIXME: probably want a more roboust splitter
    val reports = service.getProjectReport(releaseName, Lists.newArrayList(projects.split(",")));
    return Response.ok(reports).build();
  }

  @GET
  @Path("{releaseName}")
  public Response test(@PathParam("releaseName") String releaseName) {
    val reports = service.getExecutiveReport(releaseName);

    return Response.ok(reports).build();
  }
}
