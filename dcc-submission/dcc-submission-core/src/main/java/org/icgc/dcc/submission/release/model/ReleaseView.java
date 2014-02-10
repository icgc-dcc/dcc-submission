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

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Maps.newHashMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import lombok.val;

import org.hibernate.validator.constraints.NotBlank;
import org.icgc.dcc.submission.core.model.SubmissionFile;
import org.icgc.dcc.submission.release.ReleaseException;

/**
 * 
 */
public class ReleaseView {

  @NotBlank
  protected String name;

  protected ReleaseState state;

  @Valid
  protected List<DetailedSubmission> submissions = new ArrayList<DetailedSubmission>();

  protected List<String> queue = new ArrayList<String>();

  protected Date releaseDate;

  @NotBlank
  protected String dictionaryVersion;

  protected Map<SubmissionState, Integer> summary = newHashMap();

  public ReleaseView() {

  }

  public ReleaseView(Release release, List<LiteProject> liteProjects,
      Map<String, List<SubmissionFile>> submissionFilesMap) {

    this.name = release.name;
    this.state = release.state;
    this.queue = release.getQueuedProjectKeys();
    this.releaseDate = release.releaseDate;
    this.dictionaryVersion = release.dictionaryVersion;
    for (LiteProject liteProject : liteProjects) {
      String projectKey = liteProject.getKey();
      val optional = release.getSubmission(projectKey);
      checkState(optional.isPresent(), "Could not find project '%s' in release '%s'", projectKey, name);

      val submission = optional.get();
      DetailedSubmission detailedSubmission = new DetailedSubmission(submission, liteProject);
      detailedSubmission.setSubmissionFiles(submissionFilesMap.get(projectKey));
      this.submissions.add(detailedSubmission);

      Integer stateCount = this.summary.get(detailedSubmission.getState());
      if (stateCount == null) {
        stateCount = 0;
      }
      stateCount++;
      this.summary.put(detailedSubmission.getState(), stateCount);
    }
  }

  public DetailedSubmission getDetailedSubmission(String projectKey) {
    for (val submission : submissions) {
      val match = submission.getProjectKey().equals(projectKey);
      if (match) {
        return submission;
      }
    }

    throw new ReleaseException("There is no project '%s' associated with release '%s'", projectKey, name);
  }

  public String getName() {
    return name;
  }

  public ReleaseState getState() {
    return state;
  }

  public List<DetailedSubmission> getSubmissions() {
    return submissions;
  }

  public List<String> getQueue() {
    return queue;
  }

  public Date getReleaseDate() {
    return releaseDate;
  }

  public String getDictionaryVersion() {
    return dictionaryVersion;
  }

  public Map<SubmissionState, Integer> getSummary() {
    return summary;
  }
}
