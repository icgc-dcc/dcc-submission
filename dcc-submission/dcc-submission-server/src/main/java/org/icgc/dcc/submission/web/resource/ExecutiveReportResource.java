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

import org.icgc.dcc.submission.service.ExecutiveReportService;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

@Path("executiveReports")
public class ExecutiveReportResource {

  @Inject
  private ExecutiveReportService service;

  @GET
  @Path("{releaseName}/{project}")
  public Response getReport(@PathParam("releaseName") String releaseName, @PathParam("project") String projects) {
    val splitter = Splitter.on(",").trimResults();
    val reports = service.getProjectReport(releaseName, Lists.newArrayList(splitter.split(projects)));
    return Response.ok(reports).build();
  }

  @GET
  @Path("{releaseName}")
  public Response test(@PathParam("releaseName") String releaseName) {
    val reports = service.getExecutiveReport(releaseName);
    return Response.ok(reports).build();
  }
}
