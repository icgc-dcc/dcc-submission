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

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import org.hibernate.validator.constraints.NotBlank;
import org.icgc.dcc.submission.fs.SubmissionFile;
import org.icgc.dcc.submission.release.ReleaseException;

import static com.google.common.base.Preconditions.checkNotNull;

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

  protected Map<SubmissionState, Integer> summary = new EnumMap<SubmissionState, Integer>(SubmissionState.class);

  public ReleaseView() {

  }

  public ReleaseView(Release release, List<LiteProject> liteProjects,
      Map<String, List<SubmissionFile>> submissionFilesMap) {

    this.name = release.name;
    this.state = release.state;
    this.queue = release.getQueuedProjectKeys();
    this.releaseDate = release.releaseDate;
    this.dictionaryVersion = release.dictionaryVersion;
    for(LiteProject liteProject : liteProjects) {
      String projectKey = liteProject.getKey();
      Submission submission = checkNotNull(release.getSubmission(projectKey));

      DetailedSubmission detailedSubmission = new DetailedSubmission(submission, liteProject);
      detailedSubmission.setSubmissionFiles(submissionFilesMap.get(projectKey));
      this.submissions.add(detailedSubmission);

      Integer stateCount = this.summary.get(detailedSubmission.getState());
      if(stateCount == null) {
        stateCount = 0;
      }
      stateCount++;
      this.summary.put(detailedSubmission.getState(), stateCount);
    }
  }

  public DetailedSubmission getDetailedSubmission(String projectKey) {
    for(DetailedSubmission submission : submissions) {
      if(submission.getProjectKey().equals(projectKey)) {
        return submission;
      }
    }
    throw new ReleaseException(String.format("there is no project \"%s\" associated with release \"%s\"", projectKey,
        this.name));
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
