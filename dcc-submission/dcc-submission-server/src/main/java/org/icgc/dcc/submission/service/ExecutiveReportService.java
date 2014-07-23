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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.icgc.dcc.core.model.Dictionaries;
import org.icgc.dcc.core.model.FieldNames;
import org.icgc.dcc.core.model.FileTypes.FileType;
import org.icgc.dcc.core.util.Jackson;
import org.icgc.dcc.hadoop.dcc.SubmissionInputData;
import org.icgc.dcc.reporter.Reporter;
import org.icgc.dcc.reporter.ReporterGatherer;
import org.icgc.dcc.reporter.ReporterInput;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.repository.ProjectSequencingStrategyRepository;
import org.icgc.dcc.submission.repository.ProjectDatatypeRepository;
import org.icgc.submission.summary.ExecutiveReport;
import org.icgc.submission.summary.ProjectReport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;

@Slf4j
@RequiredArgsConstructor(onConstructor = @_(@Inject))
public class ExecutiveReportService extends AbstractExecutionThreadService {

  @NonNull
  private final ProjectDatatypeRepository projectDatatypeRepository;

  @NonNull
  private final ProjectSequencingStrategyRepository projectSequencingStrategyRepository;

  @NonNull
  private final DccFileSystem dccFileSystem;

  private static final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

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

  public List<ProjectReport> getProjectDatatypeReport() {
    return projectDatatypeRepository.findAll();
  }

  public List<ProjectReport> getProjectDatatypeReport(String releaseName, List<String> projectCodes) {
    return projectDatatypeRepository.find(releaseName, projectCodes);
  }

  public void saveProjectDatatypeReport(ProjectReport report) {
    projectDatatypeRepository.upsert(report);
  }

  public void deleteProjectDatatypeReport(final String releaseName) {
    projectDatatypeRepository.deleteByRelease(releaseName);
  }

  public List<ExecutiveReport> getProjectSequencingStrategyReport() {
    return projectSequencingStrategyRepository.findAll();
  }

  public List<ExecutiveReport> getProjectSequencingStrategyReport(String releaseName, List<String> projects) {
    return projectSequencingStrategyRepository.find(releaseName, projects);
  }

  public void saveProjectSequencingStrategyReport(ExecutiveReport report) {
    projectSequencingStrategyRepository.upsert(report);
  }

  public void deleteProjectSequencingStrategyReport(final String releaseName) {
    projectSequencingStrategyRepository.deleteByRelease(releaseName);
  }

  private ProjectReport getProjectReport(JsonNode report, String releaseName) {
    ProjectReport projectReport = new ProjectReport();
    projectReport.setReleaseName(releaseName);
    projectReport.setProjectCode(report.get("_project_id").textValue());
    projectReport.setType(report.get("_type").textValue());
    projectReport.setDonorCount(Long.parseLong(report.get("donor_id_count").textValue()));
    projectReport.setSampleCount(Long.parseLong(report.get("analyzed_sample_id_count").textValue()));
    projectReport.setSpecimenCount(Long.parseLong(report.get("specimen_id_count").textValue()));
    projectReport.setObservationCount(Long.parseLong(report.get("analysis_observation_count").textValue()));
    return projectReport;
  }

  private ExecutiveReport getExecutiveReport(JsonNode report, String releaseName) {
    val mapper = Jackson.DEFAULT;
    ExecutiveReport executiveReport = new ExecutiveReport();
    executiveReport.setReleaseName(releaseName);
    executiveReport.setProjectCode(((ObjectNode) report).remove("_project_id").textValue());
    Map<String, Long> summary = mapper.convertValue(report, new TypeReference<Map<String, Long>>() {});
    executiveReport.setCountSummary(summary);
    return executiveReport;
  }

  /**
   * Generates reports in the background
   */
  public void generateReport(
      final String releaseName,
      final List<String> projectKeys,
      final String releasePath,
      final JsonNode dictionaryNode,
      final JsonNode codelistNode) {

    log.info("Generating reports for {}", projectKeys);
    queue.add(new Runnable() {

      @Override
      public void run() {

        val patterns = Dictionaries.getPatterns(dictionaryNode);
        val matchingFiles = SubmissionInputData.getMatchingFiles(
            dccFileSystem.getFileSystem(), releasePath, projectKeys,
            patterns);
        val mappings = Dictionaries.getMapping(dictionaryNode, codelistNode, FileType.SSM_M_TYPE,
            FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_SEQUENCING_STRATEGY);
        val reporterInput = ReporterInput.from(matchingFiles);
        val projectKeysSet = Sets.<String> newHashSet(projectKeys);

        Reporter.process(releaseName, projectKeysSet, reporterInput, mappings.get());

        for (val project : projectKeys) {
          ArrayNode projectReports = ReporterGatherer.getJsonTable1(project);

          for (val report : projectReports) {
            projectDatatypeRepository.upsert(getProjectReport(report, releaseName));
          }

          ArrayNode sequencingStrategyReports = ReporterGatherer.getJsonTable2(project, mappings.get());
          for (val report : sequencingStrategyReports) {
            projectSequencingStrategyRepository.upsert(getExecutiveReport(report, releaseName));
          }

        }
      }
    });

  }
}
