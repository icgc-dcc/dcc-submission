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

import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import lombok.val;

import org.icgc.dcc.reporter.Reporter;
import org.icgc.dcc.reporter.ReporterGatherer;
import org.icgc.dcc.submission.service.ExecutiveReportService;
import org.icgc.submission.summary.ProjectReport;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;

//@Slf4j
@Path("executive_report")
public class ExecutiveReportResource {

  @Inject
  private ExecutiveReportService service;

  @GET
  @Path("/test")
  public Response testStuff() {

    new Thread(new Runnable() {

      @Override
      public void run() {
        // TODO Auto-generated method stub
        Set<String> testProjects = ImmutableSet.of("project.1", "project.2");

        Reporter.report(
            "release1",
            Optional.<Set<String>> of(testProjects),
            "/Users/dchang/workspace/dcc/dcc-submission/dcc-submission-server/src/test/resources/fixtures/submission/dcc_root_dir/release1"
            , "/Users/dchang/fake_projects.json", "/Users/dchang/fake_dictionary.json",
            "/Users/dchang/fake_codelists.json");

        for (String project : testProjects) {
          ArrayNode list = ReporterGatherer.getJsonTable1(project);

          for (Object obj : list) {
            val prObj = ((BasicDBObject) obj);

            // FIXME: Need to fix order when anthony cleans up
            ProjectReport pr = new ProjectReport();
            pr.setReleaseName("release1");
            pr.setProjectCode(prObj.getString("donor_id_count"));
            pr.setType(prObj.getString("_project_id"));
            pr.setDonorCount(Long.parseLong(prObj.getString("_type")));
            pr.setSampleCount(Long.parseLong(prObj.getString("_type")));
            pr.setSpecimenCount(Long.parseLong(prObj.getString("_type")));
            pr.setObservationCount(Long.parseLong(prObj.getString("_type")));

          }
        }

      }
    }).start();

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
    return Response.ok("Hello!").build();
  }
}
