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
package org.icgc.dcc.submission.repository;

import static org.icgc.submission.summary.QProjectReport.projectReport;

import java.util.List;

import lombok.NonNull;

import org.icgc.submission.summary.ProjectReport;
import org.icgc.submission.summary.QProjectReport;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

import com.google.inject.Inject;

public class ProjectReportRepository extends AbstractRepository<ProjectReport, QProjectReport> {

  @Inject
  public ProjectReportRepository(@NonNull Morphia morphia, @NonNull Datastore datastore) {
    super(morphia, datastore, projectReport);
  }

  public List<ProjectReport> findAll() {
    return list();
  }

  public List<ProjectReport> find(String releaseName, List<String> projectCodes) {
    return list(_.releaseName.eq(releaseName).and(_.projectCode.in(projectCodes)));
  }

  public void upsert(ProjectReport projectReport) {
    // save(projectReport);
    updateFirst(
        createQuery().filter("releaseName", projectReport.getReleaseName())
            .filter("projectCode", projectReport.getProjectCode()).filter("type", projectReport.getType()),
        projectReport, true);
  }
}
