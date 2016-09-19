/*
 * Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.
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

import static com.google.common.base.Throwables.propagate;
import static com.google.common.collect.Iterables.transform;
import static org.icgc.dcc.submission.release.model.SubmissionState.getDefaultState;

import java.io.Serializable;
import java.util.Date;
import java.util.Set;

import javax.validation.Valid;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

import org.hibernate.validator.constraints.NotBlank;
import org.icgc.dcc.common.core.model.DataType;
import org.icgc.dcc.common.core.model.Identifiable;
import org.icgc.dcc.common.core.util.Joiners;
import org.icgc.dcc.submission.core.model.Outcome;
import org.icgc.dcc.submission.core.model.Views.Digest;
import org.icgc.dcc.submission.core.report.Report;
import org.icgc.dcc.submission.core.state.DefaultStateContext;
import org.icgc.dcc.submission.core.state.StateContext;
import org.icgc.dcc.submission.fs.SubmissionFile;
import org.icgc.dcc.submission.fs.SubmissionFileEvent;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

@Slf4j
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "projectKey")
@JsonIgnoreProperties({ "id" })
public class Submission implements Serializable, Identifiable {

  private static final Joiner ID_JOINER = Joiners.HASHTAG;

  @Id
  @NotBlank
  protected String id;

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
    this.id = generateId();
  }

  /**
   * See {@link #generateId()}.
   */
  @JsonIgnore
  public static ImmutableSet<String> getProjectKeys(Set<String> submissionsIds) {
    return ImmutableSet.copyOf(transform(
        submissionsIds,
        new Function<String, String>() {

          @Override
          public String apply(@NonNull final String submissionId) {
            return getProjectKeyFromId(submissionId);
          }

          private String getProjectKeyFromId(@NonNull final String id) {
            val iterator = Joiners.getCorrespondingSplitter(ID_JOINER).split(id).iterator();
            iterator.next();
            return iterator.next();
          }

        }));
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

  public void initialize(@NonNull Iterable<SubmissionFile> submissionFiles) {
    executeTransition(submissionFiles, new Transition<Void>("initialize") {

      @Override
      public Void execute(@NonNull StateContext context) {
        state.initialize(context);
        return null;
      }

    });
  }

  public void modifyFile(@NonNull Iterable<SubmissionFile> submissionFiles, final @NonNull SubmissionFileEvent event) {
    executeTransition(submissionFiles, new Transition<Void>("modifyFile") {

      @Override
      public Void execute(@NonNull StateContext context) {
        state.modifyFile(context, event);
        return null;
      }

    });
  }

  public void queueRequest(@NonNull Iterable<SubmissionFile> submissionFiles,
      final @NonNull Iterable<DataType> dataTypes) {
    executeTransition(submissionFiles, new Transition<Void>("queueRequest") {

      @Override
      public Void execute(@NonNull StateContext context) {
        state.queueRequest(context, dataTypes);
        return null;
      }

    });
  }

  public void startValidation(@NonNull Iterable<SubmissionFile> submissionFiles,
      final @NonNull Iterable<DataType> dataTypes, final @NonNull Report nextReport) {
    executeTransition(submissionFiles, new Transition<Void>("startValidation") {

      @Override
      public Void execute(@NonNull StateContext stateContext) {
        state.startValidation(stateContext, dataTypes, nextReport);
        return null;
      }

    });
  }

  public void cancelValidation(@NonNull Iterable<SubmissionFile> submissionFiles,
      final @NonNull Iterable<DataType> dataTypes) {
    executeTransition(submissionFiles, new Transition<Void>("cancelValidation") {

      @Override
      public Void execute(@NonNull StateContext context) {
        state.cancelValidation(context, dataTypes);
        return null;
      }

    });
  }

  public void finishValidation(@NonNull Iterable<SubmissionFile> submissionFiles,
      final @NonNull Iterable<DataType> dataTypes, final @NonNull Outcome outcome, final @NonNull Report nextReport) {
    executeTransition(submissionFiles, new Transition<Void>("finishValidation") {

      @Override
      public Void execute(@NonNull StateContext context) {
        state.finishValidation(context, dataTypes, outcome, nextReport);
        return null;
      }

    });
  }

  public void signOff(@NonNull Iterable<SubmissionFile> submissionFiles) {
    executeTransition(submissionFiles, new Transition<Void>("signOff") {

      @Override
      public Void execute(@NonNull StateContext context) {
        state.signOff(context);
        return null;
      }

    });
  }

  public Submission closeRelease(@NonNull Iterable<SubmissionFile> submissionFiles, final @NonNull Release nextRelease) {
    return executeTransition(submissionFiles, new Transition<Submission>("closeRelease") {

      @Override
      public Submission execute(@NonNull StateContext context) {
        return state.closeRelease(context, nextRelease);
      }

    });
  }

  public void reset(@NonNull Iterable<SubmissionFile> submissionFiles) {
    executeTransition(submissionFiles, new Transition<Void>("reset") {

      @Override
      public Void execute(@NonNull StateContext context) {
        state.reset(context);
        return null;
      }

    });
  }

  //
  // Helpers
  //

  private StateContext createStateContext(Iterable<SubmissionFile> submissionFiles) {
    return new DefaultStateContext(this, submissionFiles);
  }

  public static Iterable<String> getProjectKeys(@NonNull Iterable<Submission> submissions) {
    return ImmutableList.copyOf(transform(submissions, getProjectKeyFunction()));
  }

  private <Result> Result executeTransition(@NonNull Iterable<SubmissionFile> submissionFiles,
      @NonNull Transition<Result> transition) {
    val action = transition.getAction();
    val stateContext = createStateContext(submissionFiles);

    try {
      log.info("Action '{}' requested starting from state '{}'", action, state);
      val result = transition.execute(stateContext);
      log.info("Finished action '{}' resulting in state '{}'", action, state);

      return result;
    } catch (Throwable t) {
      log.error("Error transitioning in response to action '" + action + "'. " + this, t);
      throw propagate(t);
    }
  }

  @RequiredArgsConstructor
  private abstract class Transition<Result> {

    @Getter
    @NonNull
    final String action;

    abstract Result execute(StateContext context);

  }

  @JsonIgnore
  public static Function<Submission, String> getProjectKeyFunction() {
    return new Function<Submission, String>() {

      @Override
      public String apply(@NonNull final Submission submission) {
        return submission.getProjectKey();
      }

    };
  }

  @JsonIgnore
  public static Predicate<Submission> isSignedOff() {
    return new Predicate<Submission>() {

      @Override
      public boolean apply(@NonNull final Submission submission) {
        return submission.getState().isSignedOff();
      }

    };
  }

  private String generateId() {
    return ID_JOINER.join(releaseName, projectKey);
  }

}
