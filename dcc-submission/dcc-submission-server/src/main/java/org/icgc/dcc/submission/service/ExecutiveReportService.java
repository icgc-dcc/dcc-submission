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
package org.icgc.dcc.submission.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.Dictionaries;
import org.icgc.dcc.core.model.FieldNames;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.reporter.Reporter;
import org.icgc.dcc.reporter.ReporterGatherer;
import org.icgc.dcc.submission.repository.ExecutiveReportRepository;
import org.icgc.dcc.submission.repository.ProjectReportRepository;
import org.icgc.submission.summary.ExecutiveReport;
import org.icgc.submission.summary.ProjectReport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;

@Slf4j
public class ExecutiveReportService {

  @NonNull
  private final ProjectReportRepository projectReportRepository;

  @NonNull
  private final ExecutiveReportRepository executiveReportRepository;

  private static final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

  private static final AbstractExecutionThreadService reportService = new AbstractExecutionThreadService() {

    @Override
    protected void run() throws Exception {
      while (isRunning()) {
        Runnable runnable = queue.take();
        if (runnable == null) {
          continue;
        }
        runnable.run();
      }
    }
  };

  @Inject
  public ExecutiveReportService(
      @NonNull ProjectReportRepository projectReportRepository,
      @NonNull ExecutiveReportRepository executiveReportRepository) {
    this.projectReportRepository = projectReportRepository;
    this.executiveReportRepository = executiveReportRepository;
    reportService.startAsync();
  }

  public List<ProjectReport> getProjectReport() {
    return projectReportRepository.findAll();
  }

  public List<ProjectReport> getProjectReport(String releaseName, List<String> projectCodes) {
    return projectReportRepository.find(releaseName, projectCodes);
  }

  public void saveProjectReport(ProjectReport report) {
    projectReportRepository.upsert(report);
  }

  public void deleteProjectReport(final String releaseName) {
    projectReportRepository.deleteByRelease(releaseName);
  }

  public List<ExecutiveReport> getExecutiveReport() {
    return executiveReportRepository.findAll();
  }

  public List<ExecutiveReport> getExecutiveReport(String releaseName) {
    return executiveReportRepository.find(releaseName);
  }

  public void saveExecutiveReport(ExecutiveReport report) {
    executiveReportRepository.upsert(report);
  }

  public void deleteExecutiveReport(final String releaseName) {
    executiveReportRepository.deleteByRelease(releaseName);
  }

  /**
   * Generates reports in the background
   */
  public void generateReport(
      final String releaseName,
      final Set<String> projectKeys,
      final String releasePath,
      final JsonNode dictionaryNode,
      final JsonNode codelistNode) {

    queue.add(new Runnable() {

      @Override
      public void run() {
        Reporter.createReport(releaseName, projectKeys, releasePath, dictionaryNode, codelistNode);

        val mapper = new ObjectMapper();
        val mappings = Dictionaries.getMapping(dictionaryNode, codelistNode, FileType.SSM_M_TYPE,
            FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_SEQUENCING_STRATEGY);

        for (String project : projectKeys) {
          ArrayNode list = ReporterGatherer.getJsonTable1(project);

          for (val obj : list) {
            // FIXME: Need to fix order when anthony cleans up reporter
            ProjectReport pr = new ProjectReport();
            pr.setReleaseName(releaseName);
            pr.setProjectCode(obj.get("donor_id_count").textValue());
            pr.setType(obj.get("_project_id").textValue());
            pr.setDonorCount(Long.parseLong(obj.get("_type").textValue()));
            pr.setSampleCount(Long.parseLong(obj.get("analyzed_sample_id_count").textValue()));
            pr.setSpecimenCount(Long.parseLong(obj.get("specimen_id_count").textValue()));
            pr.setObservationCount(Long.parseLong(obj.get("analysis_observation_count").textValue()));
            projectReportRepository.upsert(pr);
          }

          ArrayNode list2 = ReporterGatherer.getJsonTable2(project, mappings.get());
          for (val r : list2) {
            ExecutiveReport report = new ExecutiveReport();
            report.setReleaseName(releaseName);
            report.setProjectCode(((ObjectNode) r).remove("_project_id").textValue());
            Map<String, Long> summary = mapper.convertValue(r, new TypeReference<Map<String, Long>>() {});
            report.setCountSummary(summary);
            executiveReportRepository.upsert(report);
          }

        }
      }
    });

  }
}
