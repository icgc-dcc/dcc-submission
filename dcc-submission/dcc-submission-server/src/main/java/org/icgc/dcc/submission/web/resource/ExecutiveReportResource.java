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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static org.icgc.dcc.core.util.Splitters.COMMA;
import static org.icgc.dcc.submission.web.util.Authorizations.isSuperUser;
import static org.icgc.dcc.submission.web.util.Responses.unauthorizedResponse;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.util.Joiners;
import org.icgc.dcc.core.util.Strings2;
import org.icgc.dcc.submission.service.ExecutiveReportService;
import org.icgc.submission.summary.ProjectDataTypeReport;
import org.icgc.submission.summary.ProjectSequencingStrategyReport;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.inject.Inject;

@Slf4j
@Path("executiveReports")
public class ExecutiveReportResource {

  @Inject
  private ExecutiveReportService service;

  private static final Ordering<ProjectDataTypeReport> PROJECT_DATATYPE = new Ordering<ProjectDataTypeReport>() {

    @Override
    public int compare(ProjectDataTypeReport left, ProjectDataTypeReport right) {
      int result = left.getProjectCode().compareTo(right.getProjectCode());
      if (result == 0) {
        return left.getFeatureType().compareTo(right.getFeatureType());
      }
      return result;
    }
  };

  @SneakyThrows
  private void addTSVRow(Appendable appendable, Object... objects) {
    Joiners.TAB.appendTo(appendable, objects);
    appendable.append(Strings2.UNIX_NEW_LINE);
  }

  /**
   * Generates executive reports for the given release.
   */
  @POST
  // TODO: best verb (see DCC-2445)?
  @Path("/generate/{releaseName}")
  public Response generateExecutiveReport(
      @Context SecurityContext securityContext,
      @PathParam("releaseName") String releaseName) {

    log.info("Generating reports for '{}'...", releaseName);
    if (!isSuperUser(securityContext)) {
      return unauthorizedResponse();
    }

    service.generateReport(releaseName);

    return Response.ok().build();
  }

  /**
   * Generates executive reports for the given release/project combination.
   * 
   * @param projectKeys Comma-separated list of existing unique project keys.
   */
  @POST
  // TODO: best verb (see DCC-2445)?
  @Path("/generate/{releaseName}/{projectKeys}")
  public Response generateExecutiveReports(
      @Context SecurityContext securityContext,
      @PathParam("releaseName") @NonNull String releaseName,
      @PathParam("projectKeys") @NonNull String projectKeys) {

    log.info("Generating reports for '{}.{}'...", releaseName, projectKeys);
    if (!isSuperUser(securityContext)) {
      return unauthorizedResponse();
    }

    service.generateReport(releaseName, getProjectKeys(projectKeys));

    return Response.ok().build();
  }

  private Set<String> getProjectKeys(@NonNull final String projectKeys) {
    val split = COMMA.split(projectKeys);
    checkState(newArrayList(split).size() == newLinkedHashSet(split).size(),
        "Non-unique set of project keys: '%s'", projectKeys);

    return ImmutableSet.copyOf(split);
  }

  @GET
  @Path("projectDataTypeEntity/{releaseName}")
  public Response getProjectDataTypeReport(
      @PathParam("releaseName") String releaseName,
      @QueryParam("projects") List<String> projects,
      @Context HttpHeaders httpHeaders) {

    val reports = service.getProjectDataTypeReport(releaseName,
        Objects.firstNonNull(projects, Collections.<String> emptyList()));

    val header =
        ImmutableList.<String> builder().add(
            "Release", "Project Id", "Type", "Sample Type", "Donor Count", "Specimen Count", "Sample Count",
            "Observation Count");

    val dataTypeTotals = Maps.<String, ProjectDataTypeReport> newHashMap();

    // Compute totals
    for (val report : reports) {
      if (!dataTypeTotals.containsKey(report.getFeatureType())) {
        dataTypeTotals.put(report.getFeatureType(),
            new ProjectDataTypeReport(releaseName, "Total", report.getFeatureType(), report.getSampleType()));
      }
      val typeTotalReport = dataTypeTotals.get(report.getFeatureType());
      typeTotalReport.setDonorCount(typeTotalReport.getDonorCount() + report.getDonorCount());
      typeTotalReport.setSpecimenCount(typeTotalReport.getSpecimenCount() + report.getSpecimenCount());
      typeTotalReport.setSampleCount(typeTotalReport.getSampleCount() + report.getSampleCount());
      typeTotalReport.setObservationCount(typeTotalReport.getObservationCount() + report.getObservationCount());
    }

    // Order by project, then by data type. Totals always go last
    val sortedReports = PROJECT_DATATYPE.sortedCopy(reports);
    val sortedTotal = PROJECT_DATATYPE.sortedCopy(dataTypeTotals.values());
    sortedReports.addAll(sortedTotal);

    // Returns either JSON or TSV
    if (httpHeaders.getHeaderString("Accept").equals(MediaType.APPLICATION_JSON)) {
      return Response.ok(sortedReports).type(MediaType.APPLICATION_JSON).build();
    } else {
      val result = new StringBuilder();
      addTSVRow(result, header.build().toArray());
      for (val report : sortedReports) {
        addTSVRow(result, report.getReleaseName(),
            report.getProjectCode(),
            report.getFeatureType(),
            report.getSampleType(),
            String.valueOf(report.getDonorCount()),
            String.valueOf(report.getSpecimenCount()),
            String.valueOf(report.getSampleCount()),
            String.valueOf(report.getObservationCount()));
      }
      return Response.ok(result.toString()).type(MediaType.TEXT_PLAIN).build();
    }

  }

  @GET
  @Path("projectSequencingStrategy/{releaseName}")
  public Response getProjectSequencingStrategyReport(
      @PathParam("releaseName") String releaseName,
      @QueryParam("projects") List<String> projects,
      @Context HttpHeaders httpHeaders) {

    val reports = service.getProjectSequencingStrategyReport(releaseName,
        Objects.firstNonNull(projects, Collections.<String> emptyList()));
    val staticHeader = ImmutableList.<String> of("Release", "Project Id");

    // Headers may vary across releases, but within a specific release we expect to have
    // the same header for all its projects since they use the same dictionary and codelists.
    // If not, we have bigger problems...
    val dynamicHeader = ImmutableSortedSet.<String> naturalOrder();

    val reportBuilder = ImmutableList.<ProjectSequencingStrategyReport> builder();
    if (!reports.isEmpty()) {
      val total = Maps.<String, Long> newHashMap();
      dynamicHeader.addAll(reports.get(0).getCountSummary().keySet());

      // Calculate overall total
      val keys = dynamicHeader.build();
      for (val report : reports) {
        for (val key : keys.toArray(new String[keys.size()])) {
          val count = report.getCountSummary().get(key);
          if (!total.containsKey(key)) {
            total.put(key, 0L);
          }
          total.put(key, total.get(key) + count.longValue());
        }
      }
      ProjectSequencingStrategyReport totalReport = new ProjectSequencingStrategyReport();
      totalReport.setReleaseName(releaseName);
      totalReport.setProjectCode("Total");
      totalReport.setCountSummary(total);
      reportBuilder.addAll(reports).add(totalReport);
    }

    // Returns either JSON or TSV
    if (httpHeaders.getHeaderString("Accept").equals(MediaType.APPLICATION_JSON)) {
      return Response.ok(reportBuilder.build()).type(MediaType.APPLICATION_JSON).build();
    } else {
      val result = new StringBuilder();
      val header = ImmutableList.<String> builder().addAll(staticHeader).addAll(dynamicHeader.build()).build();
      addTSVRow(result, header.toArray());

      val keys = dynamicHeader.build();
      for (val report : reportBuilder.build()) {
        val detail = ImmutableList.<String> builder();
        detail.add(report.getReleaseName(), report.getProjectCode());
        for (val key : keys.toArray(new String[keys.size()])) {
          detail.add(report.getCountSummary().get(key).toString());
        }
        addTSVRow(result, detail.build().toArray());
      }
      return Response.ok(result.toString()).type(MediaType.TEXT_PLAIN).build();
    }

  }
}
