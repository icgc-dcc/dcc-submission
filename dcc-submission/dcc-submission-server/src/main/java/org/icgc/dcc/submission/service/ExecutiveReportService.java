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

import static org.icgc.dcc.core.model.Configurations.HADOOP_KEY;

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
import org.icgc.dcc.submission.repository.ProjectDataTypeReportRepository;
import org.icgc.dcc.submission.repository.ProjectSequencingStrategyReportRepository;
import org.icgc.submission.summary.ProjectDataTypeReport;
import org.icgc.submission.summary.ProjectSequencingStrategyReport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.inject.Inject;
import com.google.inject.name.Named;

@Slf4j
@RequiredArgsConstructor(onConstructor = @_(@Inject))
public class ExecutiveReportService extends AbstractExecutionThreadService {

  @NonNull
  private final ProjectDataTypeReportRepository projectDataTypeRepository;

  @NonNull
  private final ProjectSequencingStrategyReportRepository projectSequencingStrategyRepository;

  @NonNull
  private final DccFileSystem dccFileSystem;

  @NonNull
  @Named(HADOOP_KEY)
  private final Map<String, String> hadoopProperties;

  private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<Runnable>();

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

  public List<ProjectDataTypeReport> getProjectDataTypeReport() {
    return projectDataTypeRepository.findAll();
  }

  public List<ProjectDataTypeReport> getProjectDataTypeReport(String releaseName, List<String> projectCodes) {
    return projectDataTypeRepository.find(releaseName, projectCodes);
  }

  public void saveProjectDataTypeReport(ProjectDataTypeReport report) {
    projectDataTypeRepository.upsert(report);
  }

  public void deleteProjectDataTypeReport(final String releaseName) {
    projectDataTypeRepository.deleteByRelease(releaseName);
  }

  public List<ProjectSequencingStrategyReport> getProjectSequencingStrategyReport() {
    return projectSequencingStrategyRepository.findAll();
  }

  public List<ProjectSequencingStrategyReport> getProjectSequencingStrategyReport(String releaseName,
      List<String> projects) {
    return projectSequencingStrategyRepository.find(releaseName, projects);
  }

  public void saveProjectSequencingStrategyReport(ProjectSequencingStrategyReport report) {
    projectSequencingStrategyRepository.upsert(report);
  }

  public void deleteProjectSequencingStrategyReport(final String releaseName) {
    projectSequencingStrategyRepository.deleteByRelease(releaseName);
  }

  private ProjectDataTypeReport getProjectReport(JsonNode report, String releaseName) {
    ProjectDataTypeReport projectDataTypeReport = new ProjectDataTypeReport();
    projectDataTypeReport.setReleaseName(releaseName);
    projectDataTypeReport.setProjectCode(report.get("_project_id").textValue());
    projectDataTypeReport.setType(report.get("_type").textValue());
    projectDataTypeReport.setDonorCount(Long.parseLong(report.get("donor_id_count").textValue()));
    projectDataTypeReport.setSampleCount(Long.parseLong(report.get("analyzed_sample_id_count").textValue()));
    projectDataTypeReport.setSpecimenCount(Long.parseLong(report.get("specimen_id_count").textValue()));
    projectDataTypeReport.setObservationCount(Long.parseLong(report.get("analysis_observation_count").textValue()));
    return projectDataTypeReport;
  }

  private ProjectSequencingStrategyReport getExecutiveReport(JsonNode report, String releaseName) {
    val mapper = Jackson.DEFAULT;
    ProjectSequencingStrategyReport projectSequencingStrategyReport = new ProjectSequencingStrategyReport();
    projectSequencingStrategyReport.setReleaseName(releaseName);
    projectSequencingStrategyReport.setProjectCode(((ObjectNode) report).remove("_project_id").textValue());
    Map<String, Long> summary = mapper.convertValue(report, new TypeReference<Map<String, Long>>() {});
    projectSequencingStrategyReport.setCountSummary(summary);
    return projectSequencingStrategyReport;
  }

  /**
   * Generates reports in the background
   */
  public void generateReport(
      @NonNull final String releaseName,
      @NonNull final List<String> projectKeys,
      @NonNull final String releasePath,
      @NonNull final JsonNode dictionaryNode,
      @NonNull final JsonNode codeListsNode) {

    log.info("Generating reports for {}", projectKeys);

    val patterns = Dictionaries.getPatterns(dictionaryNode);
    val mappings = Dictionaries.getMapping(dictionaryNode, codeListsNode, FileType.SSM_M_TYPE,
        FieldNames.SubmissionFieldNames.SUBMISSION_OBSERVATION_SEQUENCING_STRATEGY);

    queue.add(new Runnable() {

      @Override
      public void run() {

        val matchingFiles = SubmissionInputData.getMatchingFiles(
            dccFileSystem.getFileSystem(), releasePath, projectKeys,
            patterns);
        val reporterInput = ReporterInput.from(matchingFiles);
        val projectKeysSet = Sets.<String> newHashSet(projectKeys);

        val outputDirPath = Reporter.process(
            releaseName, projectKeysSet, reporterInput, mappings.get(), ImmutableMap.copyOf(hadoopProperties));

        for (val project : projectKeys) {
          ArrayNode projectReports = ReporterGatherer.getJsonTable1(outputDirPath, project);

          for (val report : projectReports) {
            projectDataTypeRepository.upsert(getProjectReport(report, releaseName));
          }

          ArrayNode sequencingStrategyReports = ReporterGatherer.getJsonTable2(outputDirPath, project, mappings.get());
          for (val report : sequencingStrategyReports) {
            projectSequencingStrategyRepository.upsert(getExecutiveReport(report, releaseName));
          }

        }
      }
    });

  }
}
