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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.val;

import org.icgc.dcc.submission.core.model.Project;
import org.icgc.dcc.submission.fs.SubmissionFile;
import org.icgc.dcc.submission.release.ReleaseException;

@NoArgsConstructor
@Getter
@ToString
public class ReleaseView {

  protected String name;
  protected ReleaseState state;
  protected List<DetailedSubmission> submissions = new ArrayList<DetailedSubmission>();
  protected List<String> queue = new ArrayList<String>();
  protected Date releaseDate;
  protected String dictionaryVersion;
  protected Map<SubmissionState, Integer> summary = newHashMap();

  public ReleaseView(Release release, List<Project> projects, Map<String, List<SubmissionFile>> submissionFiles) {
    this.name = release.name;
    this.state = release.state;
    this.queue = release.getQueuedProjectKeys();
    this.releaseDate = release.releaseDate;
    this.dictionaryVersion = release.dictionaryVersion;

    for (val project : projects) {
      String projectKey = project.getKey();
      val optional = release.getSubmission(projectKey);
      checkState(optional.isPresent(), "Could not find project '%s' in release '%s'", projectKey, name);

      val submission = optional.get();
      DetailedSubmission detailedSubmission = new DetailedSubmission(submission, project);
      detailedSubmission.setSubmissionFiles(submissionFiles.get(projectKey));
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

}
