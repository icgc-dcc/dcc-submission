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

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.submission.service.ExecutiveReportService;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

@Slf4j
@Path("executiveReports")
@Produces("application/json")
public class ExecutiveReportResource {

  @Inject
  private ExecutiveReportService service;

  @GET
  @Path("projectDatatype/{releaseName}")
  public Response getProjectDatatypeReport(
      @PathParam("releaseName") String releaseName,
      @QueryParam("projects") List<String> projects) {

    val reports = service.getProjectDatatypeReport(releaseName,
        Objects.firstNonNull(projects, Lists.<String> newArrayList()));
    return Response.ok(reports).build();
  }

  @GET
  @Path("projectSequencingStrategy/{releaseName}")
  public Response getProjectSequencingStrategyReport(
      @PathParam("releaseName") String releaseName,
      @QueryParam("projects") List<String> projects) {
    val reports = service.getProjectSequencingStrategyReport(releaseName,
        Objects.firstNonNull(projects, Lists.<String> newArrayList()));
    return Response.ok(reports).build();
  }
}
