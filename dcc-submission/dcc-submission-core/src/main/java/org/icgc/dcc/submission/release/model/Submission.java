/*
 * Copyright (c) 2013 The Ontario Institute for Cancer Research. All rights reserved.
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
package org.icgc.dcc.submission.release.model;

import static com.google.common.collect.Iterables.transform;
import static org.icgc.dcc.submission.release.model.SubmissionState.getDefaultState;

import java.io.Serializable;
import java.util.Date;

import javax.validation.Valid;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.apache.hadoop.fs.Path;
import org.codehaus.jackson.map.annotate.JsonView;
import org.hibernate.validator.constraints.NotBlank;
import org.icgc.dcc.core.model.DataType;
import org.icgc.dcc.submission.core.model.Outcome;
import org.icgc.dcc.submission.core.model.SubmissionFile;
import org.icgc.dcc.submission.core.model.Views.Digest;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.core.state.DefaultStateContext;
import org.icgc.dcc.submission.core.state.StateContext;
import org.mongodb.morphia.annotations.Embedded;

import com.google.common.base.Function;
import com.google.common.base.Optional;

@Slf4j
@Data
@Embedded
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "projectKey")
public class Submission implements Serializable {

  @NotBlank
  @JsonView(Digest.class)
  protected String projectKey; // TODO: make those private, DetailedSubmission shouldn't extend Submission (DCC-721)

  @NotBlank
  @JsonView(Digest.class)
  protected String projectName;

  @NotBlank
  @JsonView(Digest.class)
  protected String releaseName;

  protected Date lastUpdated;

  protected SubmissionState state = getDefaultState();

  @Valid
  protected Report report = new Report();

  public Submission(@NonNull String projectKey, @NonNull String projectName, @NonNull String releaseName) {
    this(projectKey, projectName, releaseName, getDefaultState());
  }

  public Submission(@NonNull Submission other) {
    this.projectKey = other.projectKey;
    this.projectName = other.projectName;
    this.releaseName = other.releaseName;
    this.state = other.state;
    this.lastUpdated = new Date();
  }

  public Submission(@NonNull String projectKey, @NonNull String projectName, @NonNull String releaseName,
      @NonNull SubmissionState state) {
    this.projectKey = projectKey;
    this.projectName = projectName;
    this.releaseName = releaseName;
    this.state = state;
    this.lastUpdated = new Date();
  }

  //
  // Accessors
  //

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public Report getReport() {
    return report;
  }

  public void setReport(Report report) {
    this.lastUpdated = new Date();
    this.report = report;
  }

  public SubmissionState getState() {
    return state;
  }

  public void setState(SubmissionState nextState) {
    val change = state != nextState;
    if (change) {
      log.info("'{}' changed state from '{}' to '{}'", new Object[] { projectKey, getState(), nextState });
      this.lastUpdated = new Date();
      this.state = nextState;
    }
  }

  public String getProjectName() {
    return this.projectName;
  }

  public String getProjectKey() {
    return projectKey;
  }

  public void setProjectKey(String projectKey) {
    this.lastUpdated = new Date();
    this.projectKey = projectKey;
  }

  //
  // Actions
  //

  public void initializeSubmission(@NonNull Iterable<SubmissionFile> submissionFiles) {
    state.initialize(createContext(submissionFiles));
  }

  public void modifyFile(@NonNull Iterable<SubmissionFile> submissionFiles, @NonNull Optional<Path> filePath) {
    state.modifyFile(createContext(submissionFiles), filePath);
  }

  public void queueRequest(@NonNull Iterable<SubmissionFile> submissionFiles, @NonNull Iterable<DataType> dataTypes) {
    state.queueRequest(createContext(submissionFiles), dataTypes);
  }

  public void startValidation(@NonNull Iterable<SubmissionFile> submissionFiles, @NonNull Iterable<DataType> dataTypes,
      @NonNull Report nextReport) {
    state.startValidation(createContext(submissionFiles), dataTypes, nextReport);
  }

  public void cancelValidation(@NonNull Iterable<SubmissionFile> submissionFiles, @NonNull Iterable<DataType> dataTypes) {
    state.cancelValidation(createContext(submissionFiles), dataTypes);
  }

  public void finishValidation(@NonNull Iterable<SubmissionFile> submissionFiles,
      @NonNull Iterable<DataType> dataTypes, @NonNull Outcome outcome, @NonNull Report nextReport) {
    state.finishValidation(createContext(submissionFiles), dataTypes, outcome, nextReport);
  }

  public void signOff(@NonNull Iterable<SubmissionFile> submissionFiles) {
    state.signOff(createContext(submissionFiles));
  }

  public Submission closeRelease(@NonNull Iterable<SubmissionFile> submissionFiles, @NonNull Release nextRelease) {
    return state.closeRelease(createContext(submissionFiles), nextRelease);
  }

  public void reset(@NonNull Iterable<SubmissionFile> submissionFiles) {
    state.reset(createContext(submissionFiles));
  }

  //
  // Utilities
  //

  private StateContext createContext(Iterable<SubmissionFile> submissionFiles) {
    return new DefaultStateContext(this, submissionFiles);
  }

  public static Iterable<String> getProjectKeys(@NonNull Iterable<Submission> submissions) {
    return transform(submissions, new Function<Submission, String>() {

      @Override
      public String apply(Submission submission) {
        return submission.getProjectKey();
      }

    });
  }

}
