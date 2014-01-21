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
package org.icgc.dcc.submission.release;

import static com.google.common.base.Preconditions.checkState;
import static org.icgc.dcc.hadoop.fs.HadoopUtils.lsFile;
import static org.icgc.dcc.submission.release.model.SubmissionState.ERROR;
import static org.icgc.dcc.submission.release.model.SubmissionState.INVALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.NOT_VALIDATED;
import static org.icgc.dcc.submission.release.model.SubmissionState.QUEUED;
import static org.icgc.dcc.submission.release.model.SubmissionState.SIGNED_OFF;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALID;
import static org.icgc.dcc.submission.release.model.SubmissionState.VALIDATING;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.icgc.dcc.core.model.SubmissionDataType;
import org.icgc.dcc.core.model.SubmissionDataType.SubmissionDataTypes;
import org.icgc.dcc.hadoop.fs.HadoopUtils;
import org.icgc.dcc.submission.core.model.SubmissionFile;
import org.icgc.dcc.submission.dictionary.model.Dictionary;
import org.icgc.dcc.submission.fs.DccFileSystem;
import org.icgc.dcc.submission.release.model.DataTypeState;
import org.icgc.dcc.submission.release.model.Release;
import org.icgc.dcc.submission.release.model.Submission;
import org.icgc.dcc.submission.release.model.SubmissionState;
import org.icgc.dcc.submission.validation.ValidationOutcome;
import org.icgc.dcc.submission.validation.core.SchemaReport;
import org.icgc.dcc.submission.validation.core.SubmissionReport;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

@Slf4j
@RequiredArgsConstructor
public class SubmissionManager {

  @NonNull
  private final DccFileSystem dccFileSystem;

  public void queue(Submission submission, List<SubmissionDataType> dataTypes) {
    val nextState = QUEUED;

    submission.setState(nextState);
    submission.setDataState(resolveDataState(submission, dataTypes, nextState));
  }

  public void validate(Submission submission, List<SubmissionDataType> dataTypes) {
    val nextState = VALIDATING;

    submission.setState(nextState);
    submission.setDataState(resolveDataState(submission, dataTypes, nextState));
  }

  public void resolve(Submission submission, List<SubmissionDataType> dataTypes, ValidationOutcome outcome,
      SubmissionReport submissionReport, Dictionary dictionary) {
    val previousDataState = submission.getDataState();
    val previousSubmissionReport = (SubmissionReport) submission.getReport();

    SubmissionState nextState = null;
    SubmissionReport nextSubmissionReport = null;
    List<DataTypeState> nextDataState = null;
    if (outcome == ValidationOutcome.SUCCEEDED) {
      nextDataState = resolveDataState(dictionary, dataTypes, previousDataState, submissionReport, null);
      nextState = resolveSubmissionState(nextDataState);
      nextSubmissionReport = submissionReport;
    } else if (outcome == ValidationOutcome.FAILED) {
      nextDataState = resolveDataState(dictionary, dataTypes, previousDataState, previousSubmissionReport, ERROR);
      nextState = ERROR;
      nextSubmissionReport = submissionReport;
    } else if (outcome == ValidationOutcome.CANCELLED) {
      nextDataState =
          resolveDataState(dictionary, dataTypes, previousDataState, previousSubmissionReport, NOT_VALIDATED);
      nextState = resolveSubmissionState(nextDataState);
      nextSubmissionReport = previousSubmissionReport;
    } else {
      checkState(false, "Unexpected validation outcome '%s'", outcome);
    }

    submission.setState(nextState);
    submission.setDataState(nextDataState);
    submission.setReport(nextSubmissionReport);
  }

  public void modify(@NonNull Release release, @NonNull Submission submission, @NonNull Dictionary dictionary,
      @NonNull Optional<Path> path) {
    log.info("Resetting submission for project '{}' and path '{}'", submission.getProjectKey(), path);

    // Ensure it is managed first
    if (path.isPresent()) {
      val fileName = path.get().getName();
      val managed = dictionary.getFileSchemaByFileName(fileName).isPresent();
      if (!managed) {
        log.info("Aborting reset due to file '{}' not being managed by the dictionary", path);
        return;
      }
    }

    // Initialize each available data type to not validated
    val dataTypes = Sets.<SubmissionDataType> newHashSet();
    val submissionFiles = getSubmissionFiles(release, dictionary, submission.getProjectKey());
    for (val submissionFile : submissionFiles) {
      val dataType = submissionFile.getDataType();
      if (dataType != null) {
        dataTypes.add(SubmissionDataTypes.valueOf(dataType));
      }
    }
    val nextDataState = Lists.<DataTypeState> newArrayList();
    for (val dataType : dataTypes) {
      nextDataState.add(new DataTypeState(dataType, NOT_VALIDATED));
    }

    val all = !path.isPresent();
    if (all) {
      // Reset all data types
      submission.setState(NOT_VALIDATED);
      submission.setDataState(nextDataState);
      submission.setReport(new SubmissionReport());
    } else {
      val fileName = path.get().getName();
      val fileSchema = dictionary.getFileSchemaByFileName(fileName).get();
      val fileDataType = fileSchema.getDataType();

      val transitive = fileDataType.isClinicalType();
      if (transitive) {
        // Reset all data types
        modify(release, submission, dictionary, Optional.<Path> absent());

        return;
      }

      // SubmissionReport: Reset file data type only
      SubmissionReport nextReport = new SubmissionReport();
      val previousReport = (SubmissionReport) submission.getReport();
      if (previousReport != null) {
        for (val schemaReport : previousReport.getSchemaReports()) {
          val schema = dictionary.getFileSchemaByFileName(schemaReport.getName()).get();
          val schemaDataType = schema.getDataType();

          val maintain = schemaDataType != fileDataType;
          if (maintain) {
            nextReport.addSchemaReport(schemaReport);
          }
        }
      }

      // Data state: Reset file data type only
      val previousDataState = submission.getDataState();
      for (val previousDataTypeState : previousDataState) {
        val previousDataType = previousDataTypeState.getDataType();
        val maintain = previousDataType != fileDataType && dataTypes.contains(previousDataType);
        if (maintain) {
          for (val nextDataTypeState : nextDataState) {
            if (nextDataTypeState.getDataType() == previousDataType) {
              // Copy
              nextDataTypeState.setState(previousDataTypeState.getState());
            }
          }
        }
      }

      submission.setState(NOT_VALIDATED);
      submission.setDataState(nextDataState);
      submission.setReport(nextReport);
    }
  }

  public void signOff(Submission submission) {
    val nextState = SIGNED_OFF;
    val nextDataState = Lists.<DataTypeState> newArrayList();

    for (val dataTypeState : submission.getDataState()) {
      val dataType = dataTypeState.getDataType();
      nextDataState.add(new DataTypeState(dataType, nextState));
    }

    // Update everything to signed-off
    submission.setState(nextState);
    submission.setDataState(nextDataState);
  }

  public Submission release(Release nextRelease, Submission submission) {
    val newSubmission =
        new Submission(submission.getProjectKey(), submission.getProjectName(), nextRelease.getName());
    if (submission.getState() == SIGNED_OFF) {
      // Reset
      val nextDataState = Lists.<DataTypeState> newArrayList();
      for (val dataTypeState : submission.getDataState()) {
        nextDataState.add(new DataTypeState(dataTypeState.getDataType(), SubmissionState.NOT_VALIDATED));
      }

      newSubmission.setState(NOT_VALIDATED);
      newSubmission.setDataState(nextDataState);
    } else {
      // Migrate
      newSubmission.setState(submission.getState());
      newSubmission.setReport(submission.getReport());
      newSubmission.setDataState(submission.getDataState());
    }

    return newSubmission;
  }

  private List<DataTypeState> resolveDataState(Submission submission, List<SubmissionDataType> dataTypes,
      SubmissionState nextState) {
    // Set selected data types to specified state
    val nextDataState = Lists.<DataTypeState> newArrayList();
    for (val dataTypeState : submission.getDataState()) {
      val dataType = dataTypeState.getDataType();

      val selected = dataTypes.contains(dataType);
      if (selected) {
        // Update
        nextDataState.add(new DataTypeState(dataType, nextState));
      } else {
        // Maintain
        nextDataState.add(dataTypeState);
      }
    }

    return nextDataState;
  }

  private List<DataTypeState> resolveDataState(Dictionary dictionary, List<SubmissionDataType> dataTypes,
      List<DataTypeState> previousDataState, SubmissionReport submissionReport, SubmissionState inheritedNextState) {
    val nextDataState = Lists.<DataTypeState> newArrayList();

    // Update data types that were validated
    val index = getSchemaReportsByDataType(submissionReport, dictionary);
    val resultDataTypes = index.keySet();
    for (val dataType : resultDataTypes) {
      val unchanged = !dataTypes.contains(dataType);
      if (unchanged) {
        continue;
      }

      boolean errors = false;
      for (val schemaReport : index.get(dataType)) {
        if (schemaReport.hasErrors()) {
          errors = true;

          break;
        }
      }

      SubmissionState dataTypeState = null;
      if (inheritedNextState != null) {
        dataTypeState = inheritedNextState;
      } else {
        dataTypeState = errors ? INVALID : VALID;
      }

      nextDataState.add(new DataTypeState(dataType, dataTypeState));
    }

    // Pass through data types that were not validated
    for (val previousDataTypeState : previousDataState) {
      val notValidated = !resultDataTypes.contains(previousDataTypeState.getDataType());
      if (notValidated) {
        if (previousDataTypeState.getState() == VALIDATING) {
          nextDataState.add(new DataTypeState(previousDataTypeState.getDataType(), NOT_VALIDATED));
        } else {
          nextDataState.add(previousDataTypeState);
        }
      }
    }

    return nextDataState;
  }

  private SubmissionState resolveSubmissionState(List<DataTypeState> dataState) {
    val states = Sets.<SubmissionState> newHashSet();
    for (val dataTypeState : dataState) {
      states.add(dataTypeState.getState());
    }

    // Order matters
    if (states.contains(SubmissionState.ERROR)) {
      return SubmissionState.ERROR;
    }
    if (states.contains(SubmissionState.QUEUED)) {
      return SubmissionState.QUEUED;
    }
    if (states.contains(SubmissionState.VALIDATING)) {
      return SubmissionState.VALIDATING;
    }
    if (states.contains(SubmissionState.INVALID)) {
      return SubmissionState.INVALID;
    }
    if (states.contains(SubmissionState.NOT_VALIDATED)) {
      return SubmissionState.NOT_VALIDATED;
    }

    return SubmissionState.VALID;
  }

  private List<SubmissionFile> getSubmissionFiles(Release release, Dictionary dictionary, String projectKey) {
    val submissionFiles = new ArrayList<SubmissionFile>();
    val buildProjectStringPath = new Path(dccFileSystem.buildProjectStringPath(release.getName(), projectKey));

    for (val path : lsFile(dccFileSystem.getFileSystem(), buildProjectStringPath)) {
      submissionFiles.add(getSubmissionFile(dictionary, path));
    }

    return submissionFiles;
  }

  private SubmissionFile getSubmissionFile(Dictionary dictionary, Path path) {
    val fileName = path.getName();
    val fileStatus = HadoopUtils.getFileStatus(dccFileSystem.getFileSystem(), path);
    val lastUpdate = new Date(fileStatus.getModificationTime());
    val size = fileStatus.getLen();

    val fileSchema = dictionary.getFileSchemaByFileName(fileName);
    String schemaName = null;
    String dataType = null;
    if (fileSchema.isPresent()) {
      schemaName = fileSchema.get().getName();
      dataType = fileSchema.get().getDataType().name();
    } else {
      schemaName = null;
      dataType = null;
    }

    return new SubmissionFile(fileName, lastUpdate, size, schemaName, dataType);
  }

  private Multimap<SubmissionDataType, SchemaReport> getSchemaReportsByDataType(SubmissionReport submissionReport,
      Dictionary dictionary) {
    val builder = ImmutableMultimap.<SubmissionDataType, SchemaReport> builder();
    for (val schemaReport : submissionReport.getSchemaReports()) {
      val fileName = schemaReport.getName();
      val schema = dictionary.getFileSchemaByFileName(fileName).get();
      val dataType = schema.getDataType();

      builder.put(dataType, schemaReport);
    }

    return builder.build();
  }

}
