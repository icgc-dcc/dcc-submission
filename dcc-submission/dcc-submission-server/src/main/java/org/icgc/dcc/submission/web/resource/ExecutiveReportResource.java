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

import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import lombok.val;

import org.icgc.dcc.submission.service.ExecutiveReportService;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

@Path("executiveReports")
public class ExecutiveReportResource {

  @Inject
  private ExecutiveReportService service;

  private final Joiner joiner = Joiner.on("\t");

  @GET
  @Path("projectDataType/{releaseName}")
  @Produces("text/plain")
  public Response getProjectDataTypeReport(
      @PathParam("releaseName") String releaseName,
      @QueryParam("projects") List<String> projects) {

    val reports = service.getProjectDataTypeReport(releaseName,
        Objects.firstNonNull(projects, Collections.<String> emptyList()));

    List<String> header = ImmutableList.<String> builder().add(
        "Release", "Project Id", "Type", "Donor Count", "Specimen Count", "Sample Count", "Observation Count").build();

    val result = new StringBuilder();

    result.append(joiner.join(header));
    result.append("\n");

    for (val report : reports) {
      val line = Lists.<String> newArrayList();
      line.add(report.getReleaseName());
      line.add(report.getProjectCode());
      line.add(report.getType());
      line.add(String.valueOf(report.getDonorCount()));
      line.add(String.valueOf(report.getSpecimenCount()));
      line.add(String.valueOf(report.getSampleCount()));
      line.add(String.valueOf(report.getObservationCount()));
      result.append(joiner.join(line));
      result.append("\n");
    }

    return Response.ok(result.toString()).build();
  }

  @GET
  @Path("projectSequencingStrategy/{releaseName}")
  @Produces("text/plain")
  public Response getProjectSequencingStrategyReport(
      @PathParam("releaseName") String releaseName,
      @QueryParam("projects") List<String> projects) {

    val reports = service.getProjectSequencingStrategyReport(releaseName,
        Objects.firstNonNull(projects, Collections.<String> emptyList()));
    val staticHeader = ImmutableList.<String> of("Release", "Project Id");

    val result = new StringBuilder();

    // Headers may vary across releases, but within a specific release we expect to have
    // the same header for all its projects since they use the same dictionary and codelists.
    // If not, we have bigger problems...
    if (!reports.isEmpty()) {
      val dynamicHeader =
          ImmutableSortedSet.<String> naturalOrder().addAll(reports.get(0).getCountSummary().keySet()).build();
      val header = ImmutableList.<String> builder().addAll(staticHeader).addAll(dynamicHeader).build();
      result.append(joiner.join(header));
      result.append("\n");

      for (val report : reports) {
        List<String> line = Lists.newArrayList();

        // Handle static
        line.add(report.getReleaseName());
        line.add(report.getProjectCode());

        // Handle dynamic
        for (val key : dynamicHeader) {
          line.add(report.getCountSummary().get(key).toString());
        }
        result.append(joiner.join(line));
        result.append("\n");
      }

    } else {
      result.append(joiner.join(staticHeader));
      result.append("\n");
    }

    // reports.get(0).getCountSummary().keySet()
    // List<String> dynamicHeader =

    return Response.ok(result.toString()).build();
  }
}
