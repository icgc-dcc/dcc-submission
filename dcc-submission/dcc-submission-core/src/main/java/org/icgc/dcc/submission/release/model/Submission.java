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
import static com.google.common.collect.Lists.newArrayList;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.validation.Valid;

import lombok.NonNull;

import org.codehaus.jackson.map.annotate.JsonView;
import org.hibernate.validator.constraints.NotBlank;
import org.icgc.dcc.submission.core.model.Views.Digest;

import com.google.code.morphia.annotations.Embedded;
import com.google.common.base.Function;
import com.google.common.base.Objects;

@Embedded
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

  protected SubmissionState state;

  protected List<DataTypeState> dataState;

  // DCC-799: Runtime type will be SubmissionReport. Static type is Object to untangle cyclic dependencies between
  // dcc-submission-server and dcc-submission-core.
  @Valid
  protected Object report;

  public Submission() {
    this.dataState = newArrayList();
  }

  public Submission(String projectKey, String projectName, String releaseName) {
    super();
    this.projectKey = projectKey;
    this.projectName = projectName;
    this.releaseName = releaseName;
    this.state = SubmissionState.NOT_VALIDATED;
    this.lastUpdated = new Date();
  }

  public Date getLastUpdated() {
    return lastUpdated;
  }

  public Object getReport() {
    // DCC-799: Runtime type will be SubmissionReport. Static type is Object to untangle cyclic dependencies between
    // dcc-submission-server and dcc-submission-core.
    return report;
  }

  public void setReport(Object report) {
    // DCC-799: Runtime type will be SubmissionReport. Static type is Object to untangle cyclic dependencies between
    // dcc-submission-server and dcc-submission-core.
    this.lastUpdated = new Date();
    this.report = report;
  }

  public void resetReport() {
    setReport(null);
  }

  public SubmissionState getState() {
    return state;
  }

  public void setState(SubmissionState state) {
    this.lastUpdated = new Date();
    this.state = state;
  }

  public List<DataTypeState> getDataState() {
    return dataState;
  }

  public void setDataState(List<DataTypeState> dataState) {
    this.lastUpdated = new Date();
    this.dataState = dataState;
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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((projectKey == null) ? 0 : projectKey.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Submission other = (Submission) obj;
    return Objects.equal(this.projectKey, other.projectKey);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(Submission.class) //
        .add("projectKey", this.projectKey) //
        .add("projectName", this.projectName) //
        .add("lastUpdated", this.lastUpdated) //
        .add("state", this.state) //
        .add("report", this.report) // TODO: toString for SubmissionReport
        .toString();
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
